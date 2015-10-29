#!/usr/bin/env python

import argparse
import json
import os
import sys
from jinja2 import Environment, FileSystemLoader
from urllib2 import urlopen, Request, HTTPError

from lib.rest import post_comment

def enrichJacoco(jacoco):
  result = {}
  result[u'results'] = jacoco
  coverageTypes = set([])
  for group, diffs in jacoco.iteritems():
    for classname, coverages in diffs.iteritems():
      for coverageType, number in coverages.iteritems():
        coverageTypes.add(coverageType)
  result[u'coverageTypes'] = [ coverageType for coverageType in coverageTypes ]
  result[u'coverageTypes'].sort()
  return result

if __name__ == '__main__':
  parser = argparse.ArgumentParser(description='''
    This script will post a comment about the output of the build
  ''', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
  parser.add_argument("-a", "--apiToken", help="Github api token")
  parser.add_argument("-d", "--directory", default="/home/gitguy/aggregate-metrics", help="Directory containing json files to report on")
  parser.add_argument("-r", "--repository", help="Format: owner/repo")
  parser.add_argument("-p", "--pullRequest", help="Pull request number")
  parser.add_argument("-o", "--stdout", action='store_true', default=False, help="Print to stdout instead of posting")
  args, unknown = parser.parse_known_args()
  with open('/'.join([args.directory, 'commands.json']), 'r') as f:
    commands = json.load(f)
  with open('/'.join([args.directory, 'checkstyle.json']), 'r') as f:
    checkstyle = json.load(f)
  with open('/'.join([args.directory, 'headerViolations.json']), 'r') as f:
    headerViolations = json.load(f)
  with open('/'.join([args.directory, 'tests.json']), 'r') as f:
    tests = json.load(f)
  with open('/'.join([args.directory, 'jacocoUnit.json']), 'r') as f:
    jacocoUnit = json.load(f)
  with open('/'.join([args.directory, 'jacocoIntegration.json']), 'r') as f:
    jacocoIntegration = json.load(f)

  jacocoUnit = enrichJacoco(jacocoUnit)
  jacocoUnit['header'] = "Unit test coverage change"
  jacocoIntegration = enrichJacoco(jacocoIntegration)
  jacocoIntegration['header'] = "Integration test coverage change"

  env = Environment(loader = FileSystemLoader('/'.join([os.path.dirname(os.path.realpath(__file__)), 'templates'])))

  comments = [
    env.get_template('commands.md').render(commands),
    env.get_template('checkstyle.md').render(checkstyle),
    env.get_template('headerViolations.md').render(headerViolations),
    env.get_template('tests.md').render(tests),
    env.get_template('jacoco.md').render(jacocoUnit),
    env.get_template('jacoco.md').render(jacocoIntegration)
  ]

  output = '\n'.join(comments)
  
  if not args.stdout and (not args.repository or not args.pullRequest or not args.apiToken):
    parser.print_help()
    raise Exception('repository, pullRequest, apiToken required')
  else:
    print 'Posting comment on pull request'

  if args.stdout:
    print '\n'.join(comments)
  else:
    post_comment(args.repository, args.pullRequest, args.apiToken, '\n'.join(comments))
