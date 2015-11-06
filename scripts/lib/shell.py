import logging

from subprocess import call
from types import BooleanType

class ShellHelper(object):
  def __init__(self, logger = logging.getLogger('.'.join([__name__, 'ShellHelper']))):
    self.logger = logger

  def add_args(self, args, cmd, flags):
    result = [ string for string in cmd ]
    for flag in flags:
      if flag in args and args[flag]:
        if type(args[flag]) == BooleanType:
          result.append('--' + flag)
        else:
          result.extend(['--' + flag, str(args[flag])])
    return result

  def call_and_check(self, command, error_message = None, shell = False):
    self.logger.debug(str(command))
    result = call(command, shell=shell)
    if result != 0:
      if not error_message:
        error_message = 'Failed to invoke command'
      raise Exception(error_message)

  def sudo(self, user, command, error_message = None, shell = False):
    actual_command = ['sudo', '-i', '-H', '-u', user, '--']
    actual_command.extend(command)
    self.call_and_check(actual_command, error_message, shell)
