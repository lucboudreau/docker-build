from subprocess import call

def call_and_check(command, error_message = None, shell=False):
  result = call(command, shell=shell)
  if result != 0:
    if not error_message:
      error_message = 'Failed to invoke command'
    raise Exception(error_message)
