#!/usr/bin/env python

import argparse
import json
import os
import sys
from subprocess import call
from urllib2 import urlopen, Request, HTTPError

from lib.log import configureLog
from lib.rest import get_pr_info, get_compare_info
from lib.shell import ShellHelper

class RepositoryFetcher(object):
  def __init__(self, shell):
    self.shell = shell

  def fetchAndMerge(self, args):
    repository = ['https://']
    if args.apiToken:
      repository.extend([args.apiToken, '@'])
    repository.append('github.com/')
    repositoryPrefix = ''.join(repository)
    repository.extend([args.repository, '.git'])
    repository = ''.join(repository)

    if args.pullRequest:
      pr_info = get_pr_info(args.repository, args.pullRequest, args.apiToken)
      commits = pr_info['commits']
      headLabel = pr_info['head']['label']
      baseLabel = pr_info['base']['label']
      branch_to_apply = pr_info['base']['ref']
      compare_info = get_compare_info(args.repository, headLabel, baseLabel, args.apiToken)
      reverse_compare_info = get_compare_info(args.repository, baseLabel, headLabel, args.apiToken)

      if not pr_info['merged'] and not pr_info['mergeable']:
        raise Exception('PR needs to be rebased to be mergeable')
      elif pr_info['merged']:
        raise Exception('PR has already been merged')
      merge_commit_sha = pr_info['merge_commit_sha']
    else:
      baseLabel = ':'.join([args.repository.split('/')[0], args.upstream])
      headLabel = ':'.join([args.originOwner, args.branchToBuild])
      branch_to_apply = args.upstream
      compare_info = get_compare_info(args.repository, headLabel, baseLabel, args.apiToken)
      reverse_compare_info = get_compare_info(args.repository, baseLabel, headLabel, args.apiToken)
      originRepo = repositoryPrefix + args.originOwner + '/' + args.repository.split('/')[1] + '.git'

    self.shell.call_and_check(['git', 'config', '--global', 'user.email', 'docker-buildguy@pentaho.com'], "Couldn't set email")
    self.shell.call_and_check(['git', 'config', '--global', 'user.name', 'docker-buildguy'], "Couldn't set email")
    self.shell.call_and_check(['git', 'clone', '--depth=' + str(int(reverse_compare_info['ahead_by']) + 10), '--branch', branch_to_apply, repository, 'build-dir/base' ], "Couldn't clone repo")
    with open(os.path.join(os.getcwd(), 'build-dir', 'diff.json'), 'w') as f:
      json.dump([ diffFile['filename'] for diffFile in compare_info['files'] ], f)
    self.shell.call_and_check(['cp', '-r', 'build-dir/base', 'build-dir/head' ], "Couldn't clone repo")
    os.chdir('build-dir/head')
    if args.pullRequest:
      self.shell.call_and_check(['git', 'fetch', '--depth=' + str(int(commits) + 10), 'origin', 'pull/' + str(args.pullRequest) + '/head:pullRequest'], "Couldn't fetch pr")
    else:
      self.shell.call_and_check(['git', 'fetch', '--depth=' + str(int(reverse_compare_info['ahead_by']) + 10), originRepo, args.branchToBuild + ':pullRequest'], "Couldn't fetch pr")
    self.shell.call_and_check(['git', 'merge', '--no-edit', '--no-ff', 'pullRequest'], "Couldn't merge pr")
    self.shell.call_and_check('git diff --name-only --relative origin/' + branch_to_apply + ' > checkdiff.out', 'Couldn\'t get diff files', shell= True)

if __name__ == '__main__':
  parser = argparse.ArgumentParser(description='''
    This script will pull down a pull request
  ''', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
  parser.add_argument("-a", "--apiToken", help="Github api token")
  parser.add_argument("-d", "--directory", default = '/home/gitguy', help="Working directory")
  parser.add_argument("-r", "--repository", help="Format: owner/repo")
  parser.add_argument("-p", "--pullRequest", help="Pull request number")
  parser.add_argument("-u", "--upstream", help="Upstream branch")
  parser.add_argument("-w", "--originOwner", help="Owner of origin repo")
  parser.add_argument("-t", "--branchToBuild", help="Branch to build")
  parser.add_argument("-l", "--logLevel", default='INFO', help="Log level")
  args, unknown = parser.parse_known_args()
  configureLog(args.logLevel)
  shell = ShellHelper()
  os.chdir(args.directory)
  if not args.repository:
    parser.print_help()
    raise Exception('repository argument required')
  elif not args.pullRequest and not ( args.originOwner and args.branchToBuild and args.upstream ):
    parser.print_help()
    raise Exception('Either pull request or origin owner and branch arguments required')
  else:
    print 'Fetching pr ' + str(args.pullRequest) + ' from ' + str(args.repository)
  RepositoryFetcher(shell).fetchAndMerge(args)
