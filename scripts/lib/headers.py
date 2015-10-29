import json
import mmap
import os
import re

def checkHeaders(diffPath):
  with open(diffPath) as diffFile:
    diff = json.load(diffFile)
  buildFiles = set([])
  for diffFile in diff:
    if diffFile.endswith(".java"):
      buildFiles.add(diffFile)
  buildFiles = [buildFile for buildFile in buildFiles]
  buildFiles.sort()
  violations = []
  for javaFile in buildFiles:
    if os.path.exists(javaFile):
      with open(javaFile) as f:
        s = mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ)
      if not re.search(br'copyright', s, re.IGNORECASE):
        # missing copyright
        violations.append(javaFile)
      elif not re.search(br'Licensed under the Apache License', s, re.IGNORECASE):
        # doesn't have apache license
        if not re.search(br'GNU Lesser General Public License', s, re.IGNORECASE):
          # doesn't have LGPL
          if not re.search(br'GNU General Public License', s, re.IGNORECASE):
            # doesn't have GPL
            if not re.search(br'PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL', s, re.IGNORECASE):
              # doesn't have any known license
              print "License header missing: " + javaFile
              violations.append(javaFile)
  return { 'violations' : [ { 'file' : violation } for violation in violations ] }
