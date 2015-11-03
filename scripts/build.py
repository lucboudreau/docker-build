#!/usr/bin/env python

import argparse
import json
import os
import re
import sys
import mmap
import re
from urllib2 import urlopen, Request, HTTPError

from lib.fs import find
from lib.shell import call_and_check
from lib.headers import checkHeaders
from lib.mvn import findModules
from lib.test import findFailuresAndErrorsList
from lib.jacoco import findCoverageList

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
  multimoduleOrder = []
  if project in manifest['projects']:
    if 'multimoduleOrder' in manifest['projects'][project]:
      multimoduleOrder = [module for module in manifest['projects'][project]['multimoduleOrder']]
  if get_build_prop(buildTools, buildTool, 'build-file') == 'pom.xml':
    for module in findModules(head):
      module = os.path.relpath(module, head)
      if module == '.':
        module = ''
      multimoduleOrder.append(module)
  if len(multimoduleOrder) > 0:
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

ant_test_pattern = re.compile('.*bin/reports/[^/]*test/xml.*\\.xml$')
failsafe_test_pattern = re.compile('.*/failsafe-reports/.*\\.xml$')
surefire_test_pattern = re.compile('.*/surefire-reports/.*\\.xml$')

def findTests():
  return find(lambda f: not f['isDir'] and (ant_test_pattern.match(f['name']) or surefire_test_pattern.match(f['name']) or failsafe_test_pattern.match(f['name'])))

jacoco_pattern = re.compile('.*/jacoco/.*csv$')

def findJacoco():
  return find(lambda f: not f['isDir'] and jacoco_pattern.match(f['name']))

jacoco_it_pattern = re.compile('.*/jacoco-integration/.*csv$')

def findJacocoIT():
  return find(lambda f: not f['isDir'] and jacoco_it_pattern.match(f['name']))

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
    os.mkdir(args.directory + '/aggregate-metrics/working-dir')

    with open(args.directory + '/aggregate-metrics/commands.json', 'w') as of:
      json.dump(buildCommands, of)

    os.chdir(args.directory + '/build-dir/base')

    for buildCommand in buildCommands['build']:
      call_and_check(buildCommand, "Build failed", shell = True)

    old_tests = findFailuresAndErrorsList(findTests())
    with open(args.directory + '/aggregate-metrics/working-dir/beforeTests.json', 'w') as of:
      json.dump(old_tests, of)

    old_jacoco = findCoverageList(findJacoco())
    with open(args.directory + '/aggregate-metrics/working-dir/jacocoUnitBefore.json', 'w') as of:
      json.dump(old_jacoco, of)

    old_jacoco_it = findCoverageList(findJacocoIT())
    with open(args.directory + '/aggregate-metrics/working-dir/jacocoIntegrationBefore.json', 'w') as of:
      json.dump(old_jacoco_it, of)
  finally:
    if 'finally' in buildCommands:
      call_and_check(buildCommands['finally'], "Cleanup failed", shell = True)

  try:
    os.chdir('../head')
    with open(args.directory + '/aggregate-metrics/headerViolations.json', 'w+') as violationsFile:
      json.dump(checkHeaders(args.directory + '/build-dir/diff.json'), violationsFile)

    for buildCommand in buildCommands['build']:
      call_and_check(buildCommand, "Build failed", shell = True)

    call_and_check('cat checkdiff.out | xargs /scripts/lib/checkstyle.py > ~/aggregate-metrics/checkstyle.json', 'Couldn\'t aggregate checkstyle output', shell= True)

    new_tests = findFailuresAndErrorsList(findTests())
    with open(args.directory + '/aggregate-metrics/working-dir/afterTests.json', 'w') as of:
      json.dump(new_tests, of)

    new_jacoco = findCoverageList(findJacoco())
    with open(args.directory + '/aggregate-metrics/working-dir/jacocoUnitAfter.json', 'w') as of:
      json.dump(new_jacoco, of)

    new_jacoco_it = findCoverageList(findJacocoIT())
    with open(args.directory + '/aggregate-metrics/working-dir/jacocoIntegrationAfter.json', 'w') as of:
      json.dump(new_jacoco_it, of)
    
    call_and_check('python /scripts/lib/testAggregate.py -b ~/aggregate-metrics/working-dir/beforeTests.json -a ~/aggregate-metrics/working-dir/afterTests.json > ~/aggregate-metrics/tests.json', 'Couldn\'t aggregate surefire output after merge', shell= True)
    call_and_check('python /scripts/lib/jacocoAggregate.py -b ~/aggregate-metrics/working-dir/jacocoUnitBefore.json -a ~/aggregate-metrics/working-dir/jacocoUnitAfter.json > ~/aggregate-metrics/jacocoUnit.json', 'Couldn\'t aggregate surefire output after merge', shell= True)
    call_and_check('python /scripts/lib/jacocoAggregate.py -b ~/aggregate-metrics/working-dir/jacocoIntegrationBefore.json -a ~/aggregate-metrics/working-dir/jacocoIntegrationAfter.json > ~/aggregate-metrics/jacocoIntegration.json', 'Couldn\'t aggregate surefire output after merge', shell= True)
  finally:
    if 'finally' in buildCommands:
      call_and_check(buildCommands['finally'], "Cleanup failed", shell = True)
