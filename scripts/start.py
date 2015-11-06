#!/usr/bin/env python

import argparse
import getpass
import logging
import os

from lib.fs import FsHelper
from lib.log import configureLog
from lib.rest import api_request
from lib.shell import ShellHelper

class BuildOrchestrator(object):
  def __init__(self, username, shell, fs, logger = logging.getLogger('.'.join([__name__, 'BuildOrchestrator']))):
    self.username = username
    self.shell = shell
    self.fs = fs
    self.logger = logger

  def start(self, args):
    self.logger.info(' '.join(['Performing orchestration as', self.username]))

    self.shell.sudo('gitguy', self.shell.add_args(args, ['/scripts/git.py', '-d', '/home/gitguy'], ['apiToken', 'repository', 'pullRequest', 'upstream', 'originOwner', 'branchToBuild', 'logLevel']), 'Unable to fetch git repository')

    self.logger.info('Preparing build resources')
    self.fs.chown('/home/buildguy/.m2/', 'buildguy', 'buildguy', True)
    self.fs.chown('/home/buildguy/.ivy2/', 'buildguy', 'buildguy', True)
    self.fs.chown('/home/buildguy/aggregate-metrics/', 'buildguy', 'buildguy', True)

    self.shell.call_and_check('find /home/gitguy -name \'.git\' | xargs rm -r', 'Unable to remove .git metadata folders', shell = True)
    self.shell.call_and_check(['mv', '/home/gitguy/build-dir', '/home/buildguy/build-dir'], 'Unable to move build directory to buildguy\'s home dir')
    self.fs.chown('/home/buildguy/build-dir', 'buildguy', 'buildguy', True)

    self.logger.info('Running build as buildguy')
    self.shell.sudo('buildguy', self.shell.add_args(args, ['/scripts/build.py', '-d', '/home/buildguy'], ['buildCommand', 'cleanupCommand', 'metadata', 'repository', 'logLevel']), 'Build failed')

    self.logger.info('Posting results as gitguy')
    self.fs.cp('/home/buildguy/aggregate-metrics/', '/home/gitguy/aggregate-metrics/', True)
    self.fs.chown('/home/gitguy/aggregate-metrics/', 'gitguy', 'gitguy', True) 
    self.shell.sudo('gitguy', self.shell.add_args(args, ['/scripts/updatePr.py', '-d', '/home/gitguy/aggregate-metrics'], ['apiToken', 'repository', 'pullRequest', 'stdout', 'logLevel']), 'Unable to aggregate results')
    

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
  parser.add_argument("-l", "--logLevel", default='INFO', help="Log level")
  args, unknown = parser.parse_known_args()
  configureLog(args.logLevel)
  args = vars(args)
  apiToken = os.environ.get('API_TOKEN')
  if apiToken:
    args['apiToken'] = apiToken
  shell = ShellHelper()
  BuildOrchestrator(getpass.getuser(), shell, FsHelper(shell)).start(args)
