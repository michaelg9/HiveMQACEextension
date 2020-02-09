import time
import requests
import json
import urllib3

client = requests.Session()
username = None
password = None
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


def verifyStatusCode(response):
    print('Status code %s reason %s' % (response.status_code, response.reason))
    if int(response.status_code / 100) != 2:
        print('Failed')


def registerClient(name='', uri='', host='localhost', port='8001'):
    timenow = str(int(time.time()))
    if name == '':
        name = timenow
    if uri == '':
        uri = timenow
    print('Registering %s with uri %s' % (name, uri))
    headers = {'Content-type': 'application/json'}
    payload = {"client_name": name, "client_uri": uri}
    response = client.post('https://%s:%s/api/client/dyn_client_reg' % (host, port), data=json.dumps(payload), headers=headers, verify=False)
    verifyStatusCode(response)
    responsejson = response.json()
    return responsejson['client_id'], responsejson['client_secret']

def registerUser(host='localhost', port='8001'):
    timenow = str(int(time.time()))
    global username, password
    username = timenow
    password = timenow
    print('Registering user %s with pwd %s' % (timenow, timenow))
    payload = "username=%s&password=%s" % (username, password)
    headers = {'Content-type': 'application/x-www-form-urlencoded'}
    response = client.post('https://%s:%s/api/ro/register' % (host, port), data=payload, headers=headers, verify=False)
    verifyStatusCode(response)


def loginUser(host='localhost', port='8001'):
    global username, password
    if username is None or password is None:
        registerUser(host, port)
    print('Logging user %s with pwd %s' % (username, password))
    payload = "username=%s&password=%s" % (username, password)
    headers = {'Content-type': 'application/x-www-form-urlencoded'}
    response = client.post('https://%s:%s/api/ro/login' % (host, port), data=payload, headers=headers, verify=False)
    verifyStatusCode(response)


def addPolicy(topic, client_id=None, scope=None, host='localhost', port='8001'):
    secret = ''
    if scope is None:
        scope = ['sub', 'pub']
    if client_id is None:
        client_id, secret = registerClient()
    print('Adding policy for client %s to %s in topic %s' % (client_id, scope, topic))
    payload = {"resource_name": topic, "client_id": client_id, "scopes": scope, "policy_expires_at": "Mon, 20 Apr 2020 16:45:08 GMT"}
    headers = {'Content-type': 'application/json'}
    response = client.post('https://%s:%s/api/ro/policy' % (host, port), data=json.dumps(payload), headers=headers, verify=False)
    verifyStatusCode(response)
    return client_id, secret
