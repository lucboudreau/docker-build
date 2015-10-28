#!/usr/bin/env python

import argparse
import json
import os
import sys
import mmap
import re
from subprocess import call
from urllib2 import urlopen, Request, HTTPError

def call_and_check(command, error_message, shell=False):
  result = call(command, shell=shell)
  if result != 0:
    raise Exception(error_message)

def get_build_prop(buildTools, buildTool, propName):
  result = None
  if propName in buildTools:
    result = buildTools[propName]
  if buildTool in buildTools:
    if propName in buildTools[buildTool]:
      result = buildTools[buildTool][propName]
  return result

def get_build_commands(manifest, project, diff, head):
  buildTools = manifest['build-tools']
  if project in manifest['projects']:
    manifestProject = manifest['projects'][project]
  else:
    manifestProject = {}
  if 'build-tool' in manifestProject:
    buildTool = manifestProject['build-tool']
  else:
    if os.path.isfile(os.path.join(head, 'build.xml')):
      buildTool = 'ant'
    elif os.path.isfile(os.path.join(head, 'pom.xml')):
      buildTool = 'mvn'
  buildFile = get_build_prop(buildTools, buildTool, 'build-file')
  buildCommand = get_build_prop(buildTools, buildTool, 'command')
  if 'command' in manifestProject:
    buildCommand = manifestProject['command']
  buildCommands = []
  beforeAll = get_build_prop(buildTools, buildTool, 'before-all')
  if beforeAll:
    buildCommands.append(beforeAll)
  buildRoots = []
  for root, dirs, files in os.walk(head):
    for fileName in files:
      if fileName == buildFile:
        buildRoot = os.path.relpath(root, head)
        if buildRoot == '.':
          buildRoot = ''
        buildRoots.append(buildRoot)
  buildRoots.sort()
  buildRoots.reverse()
  buildFiles = set([])
  for diffFile in diff:
    for buildRoot in buildRoots:
      if diffFile.startswith(buildRoot):
        buildFiles.add(os.path.join(buildRoot, buildFile))
        break
  buildFiles = [buildFile for buildFile in buildFiles]
  buildFiles.sort()
  if project in manifest['projects']:
    if 'multimoduleOrder' in manifest['projects'][project]:
      multimoduleOrder = [module for module in manifest['projects'][project]['multimoduleOrder']]
      buildOrder = {}
      for buildFile in buildFiles:
        lastMatch = ''
        for idx, val in enumerate(multimoduleOrder):
          if buildFile.startswith(val) and len(lastMatch) < len(val):
            buildOrder[buildFile] = idx
            lastMatch = val
        if buildFile not in buildOrder:
          buildOrder[buildFile] = len(multimoduleOrder)
      buildFiles.sort(key = lambda buildFile: ( buildOrder[buildFile], buildFile ) )
  for buildFile in buildFiles:
    buildCommands.append(buildCommand.replace('BUILD_FILE', buildFile))
  afterAll = get_build_prop(buildTools, buildTool, 'finally')
  result = { 'build': buildCommands }
  if afterAll:
    result['finally'] = afterAll
  return result



def checkHeaders(diffPath, violationsFile):
  with open(diffPath) as diffFile:
    diff = json.load(diffFile)
    buildFiles = set([])
    for diffFile in diff:
      if diffFile.endswith(".java"):
        buildFiles.add(diffFile)
    buildFiles = [buildFile for buildFile in buildFiles]
    buildFiles.sort()
    violations = []
    for javaFile in buildFiles:
      if os.path.exists(javaFile):
        f = open(javaFile)
        s = mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ)
        if not re.search(br'copyright', s, re.IGNORECASE):
          # missing copyright
          violations.append(javaFile)
        elif not re.search(br'Licensed under the Apache License', s, re.IGNORECASE):
          # doesn't have apache license
          if not re.search(br'GNU Lesser General Public License', s, re.IGNORECASE):
            # doesn't have LGPL
            if not re.search(br'GNU General Public License', s, re.IGNORECASE):
              # doesn't have GPL
              if not re.search(br'PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL', s, re.IGNORECASE):
                # doesn't have any known license
                violations.append(javaFile)
    violations_json = ''
    for violation in violations:
      print "License header missing: " + violation
      if violations_json == '':
        violations_json = '{"violations":['
      else:
        violations_json += ','
      violations_json += '{"file":"' + violation + '"}'
    if violations_json == '':
      violations_json += '{"violations":[]}'

    header_violations_file = open(violationsFile, "w+")
    header_violations_file.write(violations_json)
    header_violations_file.close()





if __name__ == '__main__':
  parser = argparse.ArgumentParser(description='''
    This script will pull down a pull request, build it, and compare it against master
  ''', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
  parser.add_argument("-b", "--buildCommand", help="Build command")
  parser.add_argument("-c", "--cleanupCommand", help="Cleanup command")
  parser.add_argument("-m", "--metadata", help="Metadata (automatic build target detection)")
  parser.add_argument("-r", "--repository", help="Repository")
  parser.add_argument("-d", "--directory", default='/home/buildguy', help="base-dir")
  args, unknown = parser.parse_known_args()
  os.chdir(args.directory + '/build-dir')
  if args.metadata:
    if not args.repository:
      raise Exception('Require project argument if metadata specified')
    with open('diff.json') as diffFile:
      buildCommands = get_build_commands(json.loads(args.metadata), args.repository, json.load(diffFile), 'head')
  elif args.buildCommand and args.cleanupCommand:
    buildCommands = { 'build': [args.buildCommand], 'finally': args.cleanupCommand }
  else:
    parser.print_help()
    raise Exception('buildCommand, and cleanupCommand are required')

  try:
    os.mkdir('/home/buildguy/aggregate-metrics/working-dir')
    with open(args.directory + '/aggregate-metrics/commands.json', 'w') as of:
      json.dump(buildCommands, of)
    os.chdir(args.directory + '/build-dir/base')
    for buildCommand in buildCommands['build']:
      call_and_check(buildCommand, "Build failed", shell = True)
    call_and_check('{ find ./ -name \'surefire-reports\' -print0 && find ./ -name \'failsafe-reports\' -print0 && find ./ -wholename \'*bin/reports/*test/xml*\' -type d -print0; } | xargs -0 -I {} find {} -name \'*.xml\' -print0 | xargs -0 python /scripts/lib/test.py > ~/aggregate-metrics/working-dir/beforeTests.json', 'Couldn\'t aggregate surefire output before merge', shell= True)
    call_and_check('find ./ -name \'jacoco\' -print0 | xargs -0 -I {} find {} -name \'*.csv\' -print0 | xargs -0 python /scripts/lib/jacoco.py  > ~/aggregate-metrics/working-dir/jacocoUnitBefore.json', 'Couldn\'t aggregate jacoco unit test output before merge', shell= True)
    call_and_check('{ find ./ -name \'jacoco-integration\' -print0; } | xargs -0 -I {} find {} -name \'*.csv\' -print0 | xargs -0 python /scripts/lib/jacoco.py  > ~/aggregate-metrics/working-dir/jacocoIntegrationBefore.json', 'Couldn\'t aggregate jacoco integration test output before merge', shell= True)
  finally:
    if 'finally' in buildCommands:
      call_and_check(buildCommands['finally'], "Cleanup failed", shell = True)

  try:
    os.chdir('../head')
    checkHeaders(args.directory + '/build-dir/diff.json', '/home/buildguy/aggregate-metrics/headerViolations.json')
    for buildCommand in buildCommands['build']:
      call_and_check(buildCommand, "Build failed", shell = True)
    call_and_check('cat checkdiff.out | xargs /scripts/lib/checkstyle.py > ~/aggregate-metrics/checkstyle.json', 'Couldn\'t aggregate checkstyle output', shell= True)
    call_and_check('{ find ./ -name \'surefire-reports\' -print0 && find ./ -name \'failsafe-reports\' -print0 && find ./ -wholename \'*bin/reports/*test/xml*\' -type d -print0; } | xargs -0 -I {} find {} -name \'*.xml\' -print0 | xargs -0 python /scripts/lib/test.py > ~/aggregate-metrics/working-dir/afterTests.json', 'Couldn\'t aggregate surefire output after merge', shell= True)
    call_and_check('find ./ -name \'jacoco\' -print0 | xargs -0 -I {} find {} -name \'*.csv\' -print0 | xargs -0 python /scripts/lib/jacoco.py  > ~/aggregate-metrics/working-dir/jacocoUnitAfter.json', 'Couldn\'t aggregate jacoco unit test output after merge', shell= True)
    call_and_check('{ find ./ -name \'jacoco-integration\' -print0; } | xargs -0 -I {} find {} -name \'*.csv\' -print0 | xargs -0 python /scripts/lib/jacoco.py  > ~/aggregate-metrics/working-dir/jacocoIntegrationAfter.json', 'Couldn\'t aggregate jacoco integration test output after merge', shell= True)
    call_and_check('python /scripts/lib/testAggregate.py -b ~/aggregate-metrics/working-dir/beforeTests.json -a ~/aggregate-metrics/working-dir/afterTests.json > ~/aggregate-metrics/tests.json', 'Couldn\'t aggregate surefire output after merge', shell= True)
    call_and_check('python /scripts/lib/jacocoAggregate.py -b ~/aggregate-metrics/working-dir/jacocoUnitBefore.json -a ~/aggregate-metrics/working-dir/jacocoUnitAfter.json > ~/aggregate-metrics/jacocoUnit.json', 'Couldn\'t aggregate surefire output after merge', shell= True)
    call_and_check('python /scripts/lib/jacocoAggregate.py -b ~/aggregate-metrics/working-dir/jacocoIntegrationBefore.json -a ~/aggregate-metrics/working-dir/jacocoIntegrationAfter.json > ~/aggregate-metrics/jacocoIntegration.json', 'Couldn\'t aggregate surefire output after merge', shell= True)
  finally:
    if 'finally' in buildCommands:
      call_and_check(buildCommands['finally'], "Cleanup failed", shell = True)
