#!/usr/bin/python

import csv
import os
import json
import sys
import xml.etree.ElementTree as ET

def findCoverage(jacocoCsv, result):
  with open(jacocoCsv, 'r') as csvFile:
    reader = csv.DictReader(csvFile)
    for row in reader:
      row_result = {}
      for column, value in row.iteritems():
        column = column.lower()
        column_split = column.split('_')
        if len(column_split) > 1:
          column = column_split[0]
          for word in column_split[1:]:
            column += word[0].upper()
            if len(word) > 1:
              column += word[1:]
          row_result[column] = value
      if row['GROUP'] not in result:
        result[row['GROUP']] = {}
      result[row['GROUP']][row['PACKAGE'] + '.' + row['CLASS']] = row_result

def findCoverageList(jacocoCsvs):
  result = {}
  for jacocoCsv in jacocoCsvs:
    findCoverage(jacocoCsv, result)
  return { k: v for k, v in result.iteritems() if len(v) > 0 }
