from shell import call_and_check

def chown(path, user, group, recursive = False):
  cmd = ['chown'] 
  if recursive:
    cmd.append('-R')
  cmd.extend([user + ':' + group, path])
  call_and_check(cmd, 'Unable to set ownership of ' + path)

def cp(fromPath, toPath, recursive = False):
  cmd = ['cp']
  if recursive:
    cmd.append('-r')
  cmd.extend([fromPath, toPath])
  call_and_check(cmd, 'Unable to copy ' + fromPath + ' to ' + toPath)
