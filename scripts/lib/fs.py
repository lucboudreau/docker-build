import os
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

def find(matches):
  for root, dirs, files in os.walk(os.path.curdir):
    if matches({'name': root, 'isDir': True}):
      yield root
    for f in files:
      relpath = os.path.relpath(os.path.join(root, f))
      if matches({'name': relpath, 'isDir': False}):
        yield relpath
