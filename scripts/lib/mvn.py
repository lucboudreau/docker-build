
from os.path import join
import xml.etree.ElementTree as ET

def findModules(directory):
  pom_list = []
  current_pom = join(directory, 'pom.xml')
  pom_list.append(directory)
  pom_tree = ET.parse(current_pom)
  for module in pom_tree.iter():
    if module.tag.endswith('}module'):
      pom_list.extend(findModules(join(directory, module.text)))
  return pom_list
