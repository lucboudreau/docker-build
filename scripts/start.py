#!/usr/bin/env python

import argparse
import os

from lib.fs import chown, cp
from lib.rest import api_request
from lib.shell import call_and_check, add_args

if __name__ == '__main__':
  parser = argparse.ArgumentParser(description='''
    This script will pull down a pull request
  ''', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
  parser.add_argument("-r", "--repository", help="Format: owner/repo")
  parser.add_argument("-p", "--pullRequest", help="Pull request number")
  parser.add_argument("-u", "--upstream", help="Upstream branch")
  parser.add_argument("-w", "--originOwner", help="Owner of origin repo")
  parser.add_argument("-t", "--branchToBuild", help="Branch to build")
  parser.add_argument("-b", "--buildCommand", help="Build command")
  parser.add_argument("-c", "--cleanupCommand", help="Cleanup command")
  parser.add_argument("-m", "--metadata", help="Metadata (automatic build target detection)")
  parser.add_argument("-o", "--stdout", action='store_true', default=False, help="Print to stdout instead of posting")
  args, unknown = parser.parse_known_args()
  args = vars(args)
  apiToken = os.environ.get('API_TOKEN')
  if apiToken:
    args['apiToken'] = apiToken

  print('Performing setup as root')

  print('Fetching git repo as gitguy')
  call_and_check(add_args(args, ['sudo', '-i', '-H', '-u', 'gitguy', '--', '/scripts/git.py', '-d', '/home/gitguy'], ['apiToken', 'repository', 'pullRequest', 'upstream', 'originOwner', 'branchToBuild']), 'Unable to fetch git repository')

  print('Preparing build resources')
  chown('/home/buildguy/.m2/', 'buildguy', 'buildguy', True)
  chown('/home/buildguy/.ivy2/', 'buildguy', 'buildguy', True)
  chown('/home/buildguy/aggregate-metrics/', 'buildguy', 'buildguy', True)

  call_and_check('find /home/gitguy -name \'.git\' | xargs rm -r', 'Unable to remove .git metadata folders', shell = True)
  call_and_check(['mv', '/home/gitguy/build-dir', '/home/buildguy/build-dir'], 'Unable to move build directory to buildguy\'s home dir')
  chown('/home/buildguy/build-dir', 'buildguy', 'buildguy', True)

  print('Running build as buildguy')
  call_and_check(add_args(args, ['sudo', '-i', '-H', '-u', 'buildguy', '--', '/scripts/build.py', '-d', '/home/buildguy'], ['buildCommand', 'cleanupCommand', 'metadata', 'repository']), 'Build failed')

  print('Posting results as gitguy')
  cp('/home/buildguy/aggregate-metrics/', '/home/gitguy/aggregate-metrics/', True)
  chown('/home/gitguy/aggregate-metrics/', 'gitguy', 'gitguy', True) 
  call_and_check(add_args(args, ['sudo', '-i', '-H', '-u', 'gitguy', '--', '/scripts/updatePr.py', '-d', '/home/gitguy/aggregate-metrics'], ['apiToken', 'repository', 'pullRequest', 'stdout']), 'Unable to aggregate results')
