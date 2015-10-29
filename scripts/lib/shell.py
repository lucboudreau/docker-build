from subprocess import call
from types import BooleanType

def add_args(args, cmd, flags):
  result = [ string for string in cmd ]
  for flag in flags:
    if flag in args and args[flag]:
      if type(args[flag]) == BooleanType:
        result.append('--' + flag)
      else:
        result.extend(['--' + flag, str(args[flag])])
  return result

def call_and_check(command, error_message = None, shell=False):
  result = call(command, shell=shell)
  if result != 0:
    if not error_message:
      error_message = 'Failed to invoke command'
    raise Exception(error_message)
