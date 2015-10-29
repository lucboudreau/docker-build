import json
from urllib2 import urlopen, Request, HTTPError

def api_request(url, token = None):
  request = Request(url)
  if token:
    request.add_header('Authorization', 'token {}'.format(token))
  response = urlopen(request, timeout = 20)
  charset = 'UTF-8'
  content_type = response.info().get('Content-Type').split(';')
  for info in content_type:
    if info.startswith('charset='):
      charset = info.split('=')[-1]
  return json.loads(response.read().decode(charset, 'strict'))

def get_pr_info(repo, pr, token = None):
  return api_request(''.join(['https://api.github.com/repos/', repo, '/pulls/', str(pr)]), token)

def get_compare_info(repo, headLabel, baseLabel, token = None):
  return api_request(''.join(['https://api.github.com/repos/', repo, '/compare/', baseLabel, '...', headLabel]), token)

def post_comment(repo, pr, token, comment):
  try:
    url = ''.join(['https://api.github.com/repos/', repo, '/issues/', str(pr), '/comments'])
    request = Request(url, json.dumps({ 'body' : comment }).encode('UTF-8'))
    request.add_header('Authorization', 'token {}'.format(token))
    response = urlopen(request, timeout = 20)
    charset = 'UTF-8'
    content_type = response.info().get('Content-Type').split(';')
    for info in content_type:
      if info.startswith('charset='):
        charset = info.split('=')[-1]
    return json.loads(response.read().decode(charset, 'strict'))
  except HTTPError as e:
    print e.read()
