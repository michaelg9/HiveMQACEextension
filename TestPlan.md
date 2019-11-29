# Interoperability and Standard compliance test plan
This file is intended to test the compliance of the ACE MQTT broker (this extension) to the [ACE MQTT TLS profile draft](https://art.tools.ietf.org/html/draft-sengul-ace-mqtt-tls-profile-04). 
The messages exchanged between the broker, client and the AS server will be detailed here and the corresponding parts of the draft that dictate their contents will be referenced accordingly.

## Table of Contents

- [Setup](#setup)
- [MQTT Version 3 Testing](#mqtt-version-3-testing)
  * [Test case 1: Successful Authentication](#test-case-1-successful-authentication)
  * [Test case 2: Unsuccessful Authentication, CONNECT missing token](#test-case-2-unsuccessful-authentication-connect-missing-token)
  * [Test case 3: Unsuccessful Authentication, CONNECT Missing POP](#test-case-3-unsuccessful-authentication-connect-missing-pop)
  * [Test case 4: Unsuccessful Authentication, CONNECT with Wrong Token](#test-case-4-unsuccessful-authentication-connect-with-wrong-token)
  * [Test case 5: Unsuccessful Authentication, CONNECT with Wrong POP](#test-case-5-unsuccessful-authentication-connect-with-wrong-pop)
- [MQTT Version 5 Testing](#mqtt-version-5-testing)
  * [Test case 1: Successful Authentication, CONNECT with complete Authentication Data](#test-case-1-successful-authentication-connect-with-complete-authentication-data)
  * [Test case 2: Successful Authentication, CONNECT using username and password](#test-case-2-successful-authentication-connect-using-username-and-password)
  * [Test case 3: Successful Authentication, Challenge AUTH](#test-case-3-successful-authentication-challenge-auth)
  * [Test case 4: Unsuccessful Authentication, AS Server discovery (Missing token and POP)](#test-case-4-unsuccessful-authentication-as-server-discovery-missing-token-and-pop)
  * [Test case 5: Unsuccessful Authentication, CONNECT with Wrong Token](#test-case-5-unsuccessful-authentication-connect-with-wrong-token)
  * [Test case 6: Unsuccessful Authentication, CONNECT with Wrong POP](#test-case-6-unsuccessful-authentication-connect-with-wrong-pop)


## Setup

AS:
- IP address: ```127.0.0.1```
- Port: ```3001```
- Policy: The only policy registered allows the below client to subscribe and publish to the topic named humidity:
```
{
  '_id': ObjectId('5dacba65de35207162708635'),
  'client_id': 'zE*ddCU6cwbFAipf',
  'owner_name': 'cs',
  'resource_name': 'humidity',
  '__v': 0,
  'policy_expires_at': ISODate('2020-02-20T16:45:08Z'),
  'scopes': ['sub', 'pub']
}
```

Client:
- IP address: ```127.0.0.1```
- ID: ```zE*ddCU6cwbFAipf```
- Secret: ```7CrGzSyzh1l/2ixRC8XfmVtXWcGDf8+Wuao8yaIsX1w=```

Broker:
- IP address: ```127.0.0.1```
- ID: ```p*6oso!eI3D2wshK```
- Secret: ```BByUwb7/FizssDcmI0AGVtIR8vvuZJR0pa7sWF7mDdw=```


## MQTT Version 3 Testing

### Test case 1: Successful Authentication
An ACE v3 client authenticates correctly, as described in the draft, with the ACE broker
#### Step 1: Client token request
The client requests an access token from the AS server, whose IP address is already known, specifying their ID and secret
in the authorization header and the resource they'd like to access and the desired actions.
```
POST /api/client/token HTTP/1.1
Content-Length: 66
Host: 127.0.0.1:3001
User-Agent: Java-http-client/12.0.1
Authorization: Basic ekUqZGRDVTZjd2JGQWlwZjo3Q3JHelN5emgxbC8yaXhSQzhYZm1WdFhXY0dEZjgrV3Vhbzh5YUlzWDF3PQ==
Content-Type: application/json

{
  'grant_type': 'client_credentials',
  'scope': 'sub',
  'aud': 'humidity'
}

```
Notes on standard compliance:  
Need to take care how to store the secrets in the client's memory safely.


#### Step 2: AS server token response
The AS server checks that the client is registered and finds the policy that authorizes the client. Thus, it issues
a client token
```
INFO: HEADERS: RESPONSE HEADERS:
    cache-control: no-store
    connection: keep-alive
    content-type: application/json
    date: Thu, 28 Nov 2019 17:26:35 GMT
    pragma: no-cache
    set-cookie: connect.sid=s%3AogjEc21o9AuIjVjMDSlmVeOv8nX5cT64.l%2BTTa3qkZZ8%2FN95bRnVH0erVe3Qgxe4xpMsh8gDwaH4; Path=/; Expires=Thu, 03 Sep 2020 17:26:35 GMT; HttpOnly
    transfer-encoding: chunked
    x-powered-by: Express

{
  'access_token': '8pqr26vpucvx2fnyadcuhge18uzz3mywmfmx9pp11d20uneafba846vnv46ztbxt9pu8ntw2t7t8gbm7dd69kavjjknv1ymmy3rmufgtw0cpe24v5ym3bthq21nzcpyp87k19h969hct7wt0tx8udb8cwyantqwm84jr46hudaggaynp8dbxnfz6xqmh6x3q40g8jeyp5ja73wf2bx27h49kz5ujkf8b859hqtv2735nh8w7xfhm8rkcz1nt2qmd5q8d3r82z1v05akd7dzj4hh4tj1rx7w7p5tpw9wrkx12hprx8td928kb461rq43cgq27c1qvdy2zb9ex2k3ymeejv8br6x84ttq332wzm3mz7fuxkxbvk1m5djh9pehzzhyrqgnymuk4kkgauf26qpjakw',
  'profile': 'mqtt_tls',
  'token_type': 'pop',
  'exp': 1574997995318,
  'cnf': {
    'jwk': {
      'kty': 'oct',
      'alg': 'HS512',
      'k': 'g6ZBouHXtecOba3YNT5y8FY90lPLxb8GgN6bSghSx6ucAcqgxtDQsaGucV+xfgXDlW9kJ6nNCy2S0iuGgOypihj85SlEGrwClLWD8Cah3UU4SuTEn9HMNzfKh4Sg6v4XwHKXs+tdCBtME+8jUduDMxqii628S2J6lmgnHCpvs58='
    }
  }
}
```

#### Step 3: Client CONNECT 
Specified by [Section 2.1.2](https://art.tools.ietf.org/html/draft-sengul-ace-mqtt-tls-profile-04#section-2.1.2). The client uses the token to connect with the broker. 
```
Received CONNECT from client 'zE*ddCU6cwbFAipf': 
    Protocol version: 'V_3_1_1', 
    Clean Start: 'true', 
    Session Expiry Interval: '0', 
    Keep Alive: '60', 
    Maximum Packet Size: '268435460', 
    Receive Maximum: '65535', 
    Topic Alias Maximum: '0', 
    Request Problem Information: 'true', 
    Request Response Information: 'false',  
    Username: '8pqr26vpucvx2fnyadcuhge18uzz3mywmfmx9pp11d20uneafba846vnv46ztbxt9pu8ntw2t7t8gbm7dd69kavjjknv1ymmy3rmufgtw0cpe24v5ym3bthq21nzcpyp87k19h969hct7wt0tx8udb8cwyantqwm84jr46hudaggaynp8dbxnfz6xqmh6x3q40g8jeyp5ja73wf2bx27h49kz5ujkf8b859hqtv2735nh8w7xfhm8rkcz1nt2qmd5q8d3r82z1v05akd7dzj4hh4tj1rx7w7p5tpw9wrkx12hprx8td928kb461rq43cgq27c1qvdy2zb9ex2k3ymeejv8br6x84ttq332wzm3mz7fuxkxbvk1m5djh9pehzzhyrqgnymuk4kkgauf26qpjakw', 
    Password (Hex): '004032363039333334444636454631463135414346433444454237464234393934333643353941313446453833424545383536394241363144463236384632423938', 
    Auth Method: 'null', 
    Auth Data (Base64): 'null', 
    User Properties: 'null'
```

Notes on standard compliance:

| Field            	| Standard value                              	| Actual Value                 	| Notes                                                                                                                       	|
|------------------	|---------------------------------------------	|------------------------------	|-----------------------------------------------------------------------------------------------------------------------------	|
| Version          	| V3.1.1                                      	| V3.1.1                       	| ✓                                                                                                                           	|
| Username flag    	| 1                                           	| 1                            	| Indicated by a non null value above                                                                                         	|
| Password flag    	| 1                                           	| 1                            	| Indicated by a non null value above                                                                                           |
| Clean start flag 	| 1                                           	| 1                            	| ✓                                                                                                                           	|
| Username         	| Access Token String                         	| Access Token String          	| ✓                                                                                                                           	|
| Password         	| MAC/DiSig over CONNECT or nonce in password 	| MAC over access token string 	| This is a simplified version. DiSig support is missing and only a given string can be used as a nonce, not the whole packet 	|


#### Step 4: Broker token introspection request
The broker introspects the access token with the AS server
```
2019-11-28 17:26:36,037 INFO  - HEADERS: REQUEST HEADERS:
POST /api/rs/introspect HTTP/1.1
Content-Length: 428
Host: 127.0.0.1:3001
User-Agent: Java-http-client/11.0.4
Authorization: Basic cCo2b3NvIWVJM0Qyd3NoSzpCQnlVd2I3L0ZpenNzRGNtSTBBR1Z0SVI4dnZ1WkpSMHBhN3NXRjdtRGR3PQ==
Content-Type: application/json
{
  'token': '8pqr26vpucvx2fnyadcuhge18uzz3mywmfmx9pp11d20uneafba846vnv46ztbxt9pu8ntw2t7t8gbm7dd69kavjjknv1ymmy3rmufgtw0cpe24v5ym3bthq21nzcpyp87k19h969hct7wt0tx8udb8cwyantqwm84jr46hudaggaynp8dbxnfz6xqmh6x3q40g8jeyp5ja73wf2bx27h49kz5ujkf8b859hqtv2735nh8w7xfhm8rkcz1nt2qmd5q8d3r82z1v05akd7dzj4hh4tj1rx7w7p5tpw9wrkx12hprx8td928kb461rq43cgq27c1qvdy2zb9ex2k3ymeejv8br6x84ttq332wzm3mz7fuxkxbvk1m5djh9pehzzhyrqgnymuk4kkgauf26qpjakw'
}
```

Notes on standard compliance:
- Implemented correctly. 
- Need to take care how the broker stores the client tokens safely.

#### Step 5: AS Server token introspection response
Specified by [Section 2.1.3](https://art.tools.ietf.org/html/draft-sengul-ace-mqtt-tls-profile-04#section-2.1.3). The AS server found the requested access token and replies with its details
```
2019-11-28 17:26:36,053 INFO  - RESPONSE: (POST http://127.0.0.1:3001/api/rs/introspect) 200 HTTP_1_1 Local port:  56273
    connection: keep-alive
    content-length: 332
    content-type: application/json; charset=utf-8
    date: Thu, 28 Nov 2019 17:26:36 GMT
    etag: W/"14c-2bgZr6Z7hj+5ldx8H1qukqW59R4"
    set-cookie: connect.sid=s%3AqBGr0sSHqU5PUD_tcRe_XyBKOXkWeq4A.BJ3di3Fyb1kFidydDegsxmlEqYgnPHyknmz5wsvf8Ec; Path=/; Expires=Thu, 03 Sep 2020 17:26:36 GMT; HttpOnly
    x-powered-by: Express

{
  'active': true,
  'profile': 'mqtt_tls',
  'exp': 1574997995,
  'sub': 'zE*ddCU6cwbFAipf',
  'aud': 'humidity',
  'scope': ['sub'],
  'cnf': {
    'jwk': {
      'alg': 'HS256',
      'kty': 'oct',
      'k': 'g6ZBouHXtecOba3YNT5y8FY90lPLxb8GgN6bSghSx6ucAcqgxtDQsaGucV+xfgXDlW9kJ6nNCy2S0iuGgOypihj85SlEGrwClLWD8Cah3UU4SuTEn9HMNzfKh4Sg6v4XwHKXs+tdCBtME+8jUduDMxqii628S2J6lmgnHCpvs58='
    }
  }
}
```

Notes on standard compliance:  
For the token validation step, the broker performs the following actions:

| Action specified by standard    | Implementation                                         | Notes                                              |
|---------------------------------|--------------------------------------------------------|----------------------------------------------------|
| Check token active              | Check active boolean value from introspection response | No support yet for self contained tokens           |
| Compute POP                     | Compute MAC over access token string                   | No support for DiSig and using packet as nonce yet |
| Cache token                     | Not yet implemented                                    |                                                    |
| Check iss, aud, nbf, iat claims | Not yet implemented                                    |                                                    |

#### Step 6: Broker CONNACK 
Specified by [Section 2.1.4](https://art.tools.ietf.org/html/draft-sengul-ace-mqtt-tls-profile-04#section-2.1.4). The broker verified that the access token string is valid and recalculated the POP proof and verified that it matches
the client's. Thus, it authenticates the client.
```
{
  returnCode=SUCCESS, 
  sessionPresent=false
}
```

Notes on standard compliance:  
Fully compliant.


### Test case 2: Unsuccessful Authentication, CONNECT missing token
Examine the messages for a client who sends a CONNECT with a missing access token
#### Step 1: Client token request
```
INFO: HEADERS: REQUEST HEADERS:
POST /api/client/token HTTP/1.1
Content-Length: 66
Host: 127.0.0.1:3001
User-Agent: Java-http-client/12.0.1
Authorization: Basic ekUqZGRDVTZjd2JGQWlwZjo3Q3JHelN5emgxbC8yaXhSQzhYZm1WdFhXY0dEZjgrV3Vhbzh5YUlzWDF3PQ==
Content-Type: application/json
{
  "grant_type":"client_credentials",
  "scope":"sub",
  "aud":"humidity"
}
```

#### Step 2: AS server token response
```
INFO: RESPONSE: (POST http://127.0.0.1:3001/api/client/token) 200 HTTP_1_1 Local port:  56341
    cache-control: no-store
    connection: keep-alive
    content-type: application/json
    date: Thu, 28 Nov 2019 17:47:31 GMT
    pragma: no-cache
    set-cookie: connect.sid=s%3AREn1Kd2FprI6Q5xHLp2i1oFKgoUOR7J1.X5YAripgCnKV6%2BofQ8B2XiE%2BnRK1A8dGEshIOQCzIdQ; Path=/; Expires=Thu, 03 Sep 2020 17:47:31 GMT; HttpOnly
    transfer-encoding: chunked
    x-powered-by: Express

{
  'access_token': 'w8uftmfn528x9jp81gx3zf3tyd9b0fcwbhvuxj5b9ydk1wb31bcjwauvfme83pf2dab6c9qpbqmrf42c16k272e8g9xe068jkkm4ww0n38f5jjwp2z8m4vb2ktf2ren5xb271hbgbntavvqxxxn262jtd72kgqybtr5jax2dh4v27fu612enn94qz0nmynkcrngw8dx30he777z2qbm4g613pt57aa755m3ayyk30evn8vwff5u25uqqr35pd9b8h3wdm63dhm4wujd596gxk2faa26zyqr2c5tgmghh85fyj7r57xwuk8tdpfn9agfx7hqx8eug2kn5u9mnukacgutr42xe2z5v96bhvh5mrmwpwvk9qfp38wnf17jvd9hbne59kh7zjkxhwkx8tqb0b7b8hg',
  'profile': 'mqtt_tls',
  'token_type': 'pop',
  'exp': 1574999251544,
  'cnf': {
    'jwk': {
      'kty': 'oct',
      'alg': 'HS512',
      'k': 'IFD9UoYrqAbP+EH/zWIC7MOoWLQxBUk4n2LjwEkt8ocS4ooUk3uKYU9AAGapSMAXLiktruEJ/raAXf91H4AaaDvT5966HV0XCp4fTr0E8x20mUYbzG7j0MJOe5J6+3ujAO9wj+FZTCGYjywNfhr3LQ/0uPlhoHQfSfG0HYpaGzk='
    }
  }
}
```

#### Step 3: Client CONNECT
The client omits to put the access token string in the CONNECT packet. The empty string signifies that the username flag
is set but the username is the empty string
```
2019-11-28 17:47:31,946 INFO  - Received CONNECT from client 'zE*ddCU6cwbFAipf': 
    Protocol version: 'V_3_1_1', 
    Clean Start: 'true', 
    Session Expiry Interval: '0', 
    Keep Alive: '60', 
    Maximum Packet Size: '268435460',
    Receive Maximum: '65535', 
    Topic Alias Maximum: '0', 
    Request Problem Information: 'true', 
    Request Response Information: 'false',  
    Username: '', 
    Password (Hex): '004038444334433436323638363341303946423038333044373838374138314337423446394244383435324346463534383743303433424643324331453037383633', 
    Auth Method: 'null', 
    Auth Data (Base64): 'null', 
    User Properties: 'null'
```

#### Step 4: Broker CONNACK
The broker figures out that the authentication data are malformed thus it doesn't contact the AS server, it just 
disauthenticates the client directly with a BAD_USER_NAME_OR_PASSWORD.
```
{
  reasonCode=BAD_USER_NAME_OR_PASSWORD, 
  sessionPresent=false
}
```


### Test case 3: Unsuccessful Authentication, CONNECT Missing POP
Examine the messages for a client who sends a CONNECT with a missing POP

#### Step 1: Client token request
```
INFO: HEADERS: REQUEST HEADERS:
POST /api/client/token HTTP/1.1
Content-Length: 66
Host: 127.0.0.1:3001
User-Agent: Java-http-client/12.0.1
Authorization: Basic ekUqZGRDVTZjd2JGQWlwZjo3Q3JHelN5emgxbC8yaXhSQzhYZm1WdFhXY0dEZjgrV3Vhbzh5YUlzWDF3PQ==
Content-Type: application/json

{
  "grant_type":"client_credentials",
  "scope":"sub",
  "aud":"humidity"
}
```

#### Step 2: AS server token response
```
INFO: RESPONSE: (POST http://127.0.0.1:3001/api/client/token) 200 HTTP_1_1 Local port:  56557
    cache-control: no-store
    connection: keep-alive
    content-type: application/json
    date: Thu, 28 Nov 2019 20:40:31 GMT
    pragma: no-cache
    set-cookie: connect.sid=s%3A5c9GZAnB9XX8yzfle-h9bDkkKn8SI7K9.%2F7fdKnRHVGSeQiTv0g4SkI6VKf6Y6VY9bTmwrEoMO%2FQ; Path=/; Expires=Thu, 03 Sep 2020 20:40:31 GMT; HttpOnly
    transfer-encoding: chunked
    x-powered-by: Express

{
  'access_token': 'g4rg402tqrkbdh7t36567d3uhkq1c5reyhfvxfmjj21auxt8yqqfv5vyaxw916dp8qcgtmub2kfe24at59x6t0ffjr1p7qqgnwgm60q7ykrprca4k539d9gmum41jkv7yz2j7tuk68vy4krv6jqc0atdqgkp24wzb8kewerzz03uwzfw57kwp8eahdadfdwqckx21t6dcpqt1b9vnxw61b5mc1jqf3zcq4m4yze1ezmt8qerm83b99zrqczp3xrd1h5x5eagdf0zvp9m96gww48wekpuqe0g80r0kztrcf29w03az5qff3zpau36ajxm7tph25005km6t1k5wnqkhfcg34mbw341y79p3t1trdkwpx1pq20byq727eu1hkrp9trhrgcq4428zdfed2491dcgm8',
  'profile': 'mqtt_tls',
  'token_type': 'pop',
  'exp': 1575009631857,
  'cnf': {
    'jwk': {
      'kty': 'oct',
      'alg': 'HS512',
      'k': 'eMmlWHww4YORrAzLBtM66uvAVaLrmXBGkCoYRFllwx58WzvxVIP2/syLEMU1kWC2cQsfHJXYOciA5dzj8bzkqkvhzSKVGeOV/kkLUOsxXAT5e/1C9lwPIe0BlujLsVZ1OLEMI0zk06/XYwrQm5EdYrvptetv+Yg0fxf1pGfNUGk='
    }
  }
}
```

#### Step 3: Client CONNECT
The client send a CONNECT packet omitting the POP from the password field. The empty string signifies that the password flag
is set but the password is empty binary data

```
2019-11-28 20:40:32,251 INFO  - Received CONNECT from client 'zE*ddCU6cwbFAipf': 
    Protocol version: 'V_3_1_1', 
    Clean Start: 'true', 
    Session Expiry Interval: '0', 
    Keep Alive: '60', 
    Maximum Packet Size: '268435460', 
    Receive Maximum: '65535', 
    Topic Alias Maximum: '0', 
    Request Problem Information: 'true', 
    Request Response Information: 'false',  
    Username: 'g4rg402tqrkbdh7t36567d3uhkq1c5reyhfvxfmjj21auxt8yqqfv5vyaxw916dp8qcgtmub2kfe24at59x6t0ffjr1p7qqgnwgm60q7ykrprca4k539d9gmum41jkv7yz2j7tuk68vy4krv6jqc0atdqgkp24wzb8kewerzz03uwzfw57kwp8eahdadfdwqckx21t6dcpqt1b9vnxw61b5mc1jqf3zcq4m4yze1ezmt8qerm83b99zrqczp3xrd1h5x5eagdf0zvp9m96gww48wekpuqe0g80r0kztrcf29w03az5qff3zpau36ajxm7tph25005km6t1k5wnqkhfcg34mbw341y79p3t1trdkwpx1pq20byq727eu1hkrp9trhrgcq4428zdfed2491dcgm8', 
    Password: '', 
    Auth Method: 'null', 
    Auth Data (Base64): 'null', 
    User Properties: 'null'
```

#### Step 4: Broker CONNACK

```
{
  reasonCode=BAD_USER_NAME_OR_PASSWORD, 
  sessionPresent=false
}
```


### Test case 4: Unsuccessful Authentication, CONNECT with Wrong Token
Examine the messages for a client who sends a CONNECT with a wrong access token string

#### Step 1: Client token request
```
INFO: HEADERS: REQUEST HEADERS:
POST /api/client/token HTTP/1.1
Content-Length: 66
Host: 127.0.0.1:3001
User-Agent: Java-http-client/12.0.1
Authorization: Basic ekUqZGRDVTZjd2JGQWlwZjo3Q3JHelN5emgxbC8yaXhSQzhYZm1WdFhXY0dEZjgrV3Vhbzh5YUlzWDF3PQ==
Content-Type: application/json

{
  "grant_type":"client_credentials",
  "scope":"sub",
  "aud":"humidity"
}
```
#### Step 2: AS server token response
```
INFO: RESPONSE: (POST http://127.0.0.1:3001/api/client/token) 200 HTTP_1_1 Local port:  56394
    cache-control: no-store
    connection: keep-alive
    content-type: application/json
    date: Thu, 28 Nov 2019 17:56:11 GMT
    pragma: no-cache
    set-cookie: connect.sid=s%3A_N1dorLqhIGYTZgQMaGnQVJ4RJTMh61i.xtD1gd0LEe7yASDKsBpzQ3gQ1jhwjiaj7GR1uiRfQVw; Path=/; Expires=Thu, 03 Sep 2020 17:56:11 GMT; HttpOnly
    transfer-encoding: chunked
    x-powered-by: Express

{
  'access_token': 'u0xvpc5chfqegvj2fjj0tf7ru20wmm8u4qh2x8ty2200jbac9yckjz2pt0kube2g79rtdeucu8ktv6wrtecya42w0wnt996mvbdmpzjgwemr0x7n3pyb9myjb78qup7dn1xbutey918nk1m7bvjy4kq9f4grrwuqug53yd2fc40arfyjc1azjzr7f8utw3yemcqbjuwm73en9xukbjmjk6fdjb5w4qb5hw3f4n953g3eumbvr971qx2yg7ckwrktkkfefq98w3kxmt543jdxc865jqhygk69nnq1qn198rm449u5bz5yypu5euhuj663q4yqb0d07guyqk5ngyhp23933z65rd5ydnpc7q8z74cpk7e6a0fuktyv20yyyyxxhr0d1rx2d7rb9h6tr8cc8gck1m',
  'profile': 'mqtt_tls',
  'token_type': 'pop',
  'exp': 1574999771663,
  'cnf': {
    'jwk': {
      'kty': 'oct',
      'alg': 'HS512',
      'k': 'GOH8RSO94lr8yGpGN9IcsTrJe5pdlWhcsoQWC67ZHTss2cngIUL/7l/GTRqabpSXJIVgGX1qoCBZBADY1zEvAGZMfz+cb4pgAbKmIRA4qKqcY+qeatYPk74dwDkQrUottk2s27Mk9N0Sg9bhZ0buTbS36/eQ7mz7s5bwPl7x5mk='
    }
  }
}
```

#### Step 3: Client CONNECT
The client appends an 'a' character to the end of the access token string.

```
2019-11-28 17:56:11,997 INFO  - Received CONNECT from client 'zE*ddCU6cwbFAipf': 
    Protocol version: 'V_3_1_1', 
    Clean Start: 'true', 
    Session Expiry Interval: '0', 
    Keep Alive: '60', 
    Maximum Packet Size: '268435460', 
    Receive Maximum: '65535', 
    Topic Alias Maximum: '0', 
    Request Problem Information: 'true', 
    Request Response Information: 'false',  
    Username: 'u0xvpc5chfqegvj2fjj0tf7ru20wmm8u4qh2x8ty2200jbac9yckjz2pt0kube2g79rtdeucu8ktv6wrtecya42w0wnt996mvbdmpzjgwemr0x7n3pyb9myjb78qup7dn1xbutey918nk1m7bvjy4kq9f4grrwuqug53yd2fc40arfyjc1azjzr7f8utw3yemcqbjuwm73en9xukbjmjk6fdjb5w4qb5hw3f4n953g3eumbvr971qx2yg7ckwrktkkfefq98w3kxmt543jdxc865jqhygk69nnq1qn198rm449u5bz5yypu5euhuj663q4yqb0d07guyqk5ngyhp23933z65rd5ydnpc7q8z74cpk7e6a0fuktyv20yyyyxxhr0d1rx2d7rb9h6tr8cc8gck1ma', 
    Password (Hex): '004031464530393437383836434232393844363346373241423642373843453833364636414641443439343136364539393943454246434632443243384543313342', 
    Auth Method: 'null', 
    Auth Data (Base64): 'null', 
    User Properties: 'null'
```

#### Step 4: Broker token introspection request
The broker introspects the access token with the AS.
```
2019-11-28 17:56:12,001 INFO  - HEADERS: REQUEST HEADERS:
POST /api/rs/introspect HTTP/1.1
Content-Length: 429
Host: 127.0.0.1:3001
User-Agent: Java-http-client/11.0.4
Authorization: Basic cCo2b3NvIWVJM0Qyd3NoSzpCQnlVd2I3L0ZpenNzRGNtSTBBR1Z0SVI4dnZ1WkpSMHBhN3NXRjdtRGR3PQ==
Content-Type: application/json

{
  "token" : "u0xvpc5chfqegvj2fjj0tf7ru20wmm8u4qh2x8ty2200jbac9yckjz2pt0kube2g79rtdeucu8ktv6wrtecya42w0wnt996mvbdmpzjgwemr0x7n3pyb9myjb78qup7dn1xbutey918nk1m7bvjy4kq9f4grrwuqug53yd2fc40arfyjc1azjzr7f8utw3yemcqbjuwm73en9xukbjmjk6fdjb5w4qb5hw3f4n953g3eumbvr971qx2yg7ckwrktkkfefq98w3kxmt543jdxc865jqhygk69nnq1qn198rm449u5bz5yypu5euhuj663q4yqb0d07guyqk5ngyhp23933z65rd5ydnpc7q8z74cpk7e6a0fuktyv20yyyyxxhr0d1rx2d7rb9h6tr8cc8gck1ma"
}
```
#### Step 5: AS Server token introspection response
The AS server didn't find such access token.
```
2019-11-28 17:56:12,005 INFO  - RESPONSE: (POST http://127.0.0.1:3001/api/rs/introspect) 200 HTTP_1_1 Local port:  56396
    connection: keep-alive
    content-length: 16
    content-type: application/json; charset=utf-8
    date: Thu, 28 Nov 2019 17:56:12 GMT
    etag: W/"10-iZ1Wee3XJp8Edii8tnDHQrctT0c"
    set-cookie: connect.sid=s%3AeYcRX3-btgrv6oi6UBdUFpUzFmLZGAdp.kKCPXwXSxGCH0miK8jPjAlwq4k0HNhpzaO21pgjlq7s; Path=/; Expires=Thu, 03 Sep 2020 17:56:12 GMT; HttpOnly
    x-powered-by: Express

{
  "active":false
}
```

#### Step 6: Broker CONNACK
The broker disauthenticates the client with a BAD_USER_NAME_OR_PASSWORD reason code.
```
{
  reasonCode=BAD_USER_NAME_OR_PASSWORD, 
  sessionPresent=false
}
```

### Test case 5: Unsuccessful Authentication, CONNECT with Wrong POP
Examine the messages for a client who sends a CONNECT with a wrong POP

#### Step 1: Client token request

```
INFO: HEADERS: REQUEST HEADERS:
POST /api/client/token HTTP/1.1
Content-Length: 66
Host: 127.0.0.1:3001
User-Agent: Java-http-client/12.0.1
Authorization: Basic ekUqZGRDVTZjd2JGQWlwZjo3Q3JHelN5emgxbC8yaXhSQzhYZm1WdFhXY0dEZjgrV3Vhbzh5YUlzWDF3PQ==
Content-Type: application/json

{
  "grant_type":"client_credentials",
  "scope":"sub",
  "aud":"humidity"
}
```

#### Step 2: AS server token response

```
INFO: RESPONSE: (POST http://127.0.0.1:3001/api/client/token) 200 HTTP_1_1 Local port:  56599
INFO: HEADERS: RESPONSE HEADERS:
    cache-control: no-store
    connection: keep-alive
    content-type: application/json
    date: Thu, 28 Nov 2019 20:47:13 GMT
    pragma: no-cache
    set-cookie: connect.sid=s%3AHyhdHYgFl6XLW-spqOpPyfmVnEb4KwBU.RymmZc9%2FZk2TbUmZkSDwQmxMFadEB4LORE%2BiskOV%2Fpw; Path=/; Expires=Thu, 03 Sep 2020 20:47:13 GMT; HttpOnly
    transfer-encoding: chunked
    x-powered-by: Express

{
  'access_token': '9fm9c19c5fez3e4rhqy7r8066rpmdub9fzc3pwd7twkh63e8p5hk7pxxd23gkw5mz2g23j2u1x3g4abm2wd3kv7bdag6je5xffa0qx7w2xe7vhu6h7zrym35d5hnpkef0z9hkvj8b3r29ewwtyv7rqc1gt9jqpww55yu9yauezjp3aexb6nezbfzv59cm25u6vmk812kn0wr8fw4wn4xx6eu6z1ce3z916ajzyj6333cxu4qrpde34uayg7pzw5m576t1kxrepdq96qrk4quv84a6fbcy3ajb12axq4fz1hvzzbtwgq5zk37j31px4at7qvmdhmwk5hvk13uzwz1cz9z1pz7jt3mfnf58dxg4mug2q8z2n7wdm63rcfn5nq4qc8fxrg00ee6fe8zn3fnjw0qrw',
  'profile': 'mqtt_tls',
  'token_type': 'pop',
  'exp': 1575010033083,
  'cnf': {
    'jwk': {
      'kty': 'oct',
      'alg': 'HS512',
      'k': 'T1po1u9bxuI6G+AfABj7uOT8XNmgr6YRt4+ahG4agINwCkGJ6w/4yOcwfxElOCLOoA0aWXLP4m+fLjXDteXP2+OYNIwYT5CPNsCrvVw1G9CgZKrxX8BhmXqb3URihO+Ukw4AKNX0hVfyvU++mQugMPjtJCCP+bdERT1S8o77318='
    }
  }
}
```

#### Step 3A: Client CONNECT with Invalid format POP, Different size than specified
The client computed the POP correctly but shortened it to 40 bytes, leaving the specified size matching the original size.

```
2019-11-28 20:47:13,525 INFO  - Received CONNECT from client 'zE*ddCU6cwbFAipf': 
    Protocol version: 'V_3_1_1', 
    Clean Start: 'true', 
    Session Expiry Interval: '0', 
    Keep Alive: '60', 
    Maximum Packet Size: '268435460', 
    Receive Maximum: '65535', 
    Topic Alias Maximum: '0', 
    Request Problem Information: 'true', 
    Request Response Information: 'false',  
    Username: '9fm9c19c5fez3e4rhqy7r8066rpmdub9fzc3pwd7twkh63e8p5hk7pxxd23gkw5mz2g23j2u1x3g4abm2wd3kv7bdag6je5xffa0qx7w2xe7vhu6h7zrym35d5hnpkef0z9hkvj8b3r29ewwtyv7rqc1gt9jqpww55yu9yauezjp3aexb6nezbfzv59cm25u6vmk812kn0wr8fw4wn4xx6eu6z1ce3z916ajzyj6333cxu4qrpde34uayg7pzw5m576t1kxrepdq96qrk4quv84a6fbcy3ajb12axq4fz1hvzzbtwgq5zk37j31px4at7qvmdhmwk5hvk13uzwz1cz9z1pz7jt3mfnf58dxg4mug2q8z2n7wdm63rcfn5nq4qc8fxrg00ee6fe8zn3fnjw0qrw', 
    Password (Hex): '00403630354434303246354544333538353035343831463646303335394544463343433038364636', 
    Auth Method: 'null', 
    Auth Data (Base64): 'null', 
    User Properties: 'null'
```

The broker figures out that the authentication data are malformed and directly disauthenticates the client, jumping to
step 6.

#### Step 3B: Client CONNECT with Invalid format POP, Miscalculated 
The client calculated the POP correctly but changed a byte in the computed POP array.
```
2019-11-28 20:53:17,454 INFO  - Received CONNECT from client 'zE*ddCU6cwbFAipf': 
    Protocol version: 'V_3_1_1', 
    Clean Start: 'true', 
    Session Expiry Interval: '0', 
    Keep Alive: '60', 
    Maximum Packet Size: '268435460', 
    Receive Maximum: '65535', 
    Topic Alias Maximum: '0', 
    Request Problem Information: 'true', 
    Request Response Information: 'false',  
    Username: 'e91wjvjkeez3ztvnmmdeufede0xg35xq93n3jy7y8j20j2t01v3mhrmg589ubf8juw80t6z5dyxbkm6kb1rk2wxhrqqk115zta2a4dhbfrv8p4dp1e9cnt5der4gnbn1wdr1r8yhn1ewy3ej58p6gccwgh2z3q6yneby5x2v0hhh2p7tdy9p0my8u73v6zdwmfjjtewdqmewpq51xnjca3wyz6e7axtyp50mn0muy36p1ypg94dwdme7pqfhp7anwku009a0rn8z1mf36rhy7n3j7h5aprc3w35pg5htverkvpeu7mxzfwa3tmxptj3wjy949ge0w2c90cuytknvxp2brcy47c58hw5fxntuuxjuyg1uugca1pxnx1r2y335nfj8p21qpbrrtvq98k13nnum2g', 
    Password (Hex): '004036424644443432393042343731383235303637383142413637464635453241384141314132370046443136393444393246413342373532413231364445433035', 
    Auth Method: 'null', 
    Auth Data (Base64): 'null', 
    User Properties: 'null'

```
#### Step 4: Broker token introspection request
The broker introspects the access token string with AS.
```
2019-11-28 20:47:13,535 INFO  - HEADERS: REQUEST HEADERS:
POST /api/rs/introspect HTTP/1.1
Content-Length: 428
Host: 127.0.0.1:3001
User-Agent: Java-http-client/11.0.4
Authorization: Basic cCo2b3NvIWVJM0Qyd3NoSzpCQnlVd2I3L0ZpenNzRGNtSTBBR1Z0SVI4dnZ1WkpSMHBhN3NXRjdtRGR3PQ==
Content-Type: application/json

{
  "token" : "9fm9c19c5fez3e4rhqy7r8066rpmdub9fzc3pwd7twkh63e8p5hk7pxxd23gkw5mz2g23j2u1x3g4abm2wd3kv7bdag6je5xffa0qx7w2xe7vhu6h7zrym35d5hnpkef0z9hkvj8b3r29ewwtyv7rqc1gt9jqpww55yu9yauezjp3aexb6nezbfzv59cm25u6vmk812kn0wr8fw4wn4xx6eu6z1ce3z916ajzyj6333cxu4qrpde34uayg7pzw5m576t1kxrepdq96qrk4quv84a6fbcy3ajb12axq4fz1hvzzbtwgq5zk37j31px4at7qvmdhmwk5hvk13uzwz1cz9z1pz7jt3mfnf58dxg4mug2q8z2n7wdm63rcfn5nq4qc8fxrg00ee6fe8zn3fnjw0qrw"
}
```

#### Step 5: AS Server token introspection response
The AS server found the access token string and responses with details about it.
```
2019-11-28 20:47:13,541 INFO  - RESPONSE: (POST http://127.0.0.1:3001/api/rs/introspect) 200 HTTP_1_1 Local port:  56601
    connection: keep-alive
    content-length: 332
    content-type: application/json; charset=utf-8
    date: Thu, 28 Nov 2019 20:47:13 GMT
    etag: W/"14c-inaJtid1O4Py1xAncu3iuaDuI1s"
    set-cookie: connect.sid=s%3AvBHb5ZnLYvnFoCOR6Irpa3eTY12m0hyv.U1D3%2FepKV5GzG%2BRegqIupodh00iyMSFqB3yrgxHPM24; Path=/; Expires=Thu, 03 Sep 2020 20:47:13 GMT; HttpOnly
    x-powered-by: Express

{
  'active': true,
  'profile': 'mqtt_tls',
  'exp': 1575010033,
  'sub': 'zE*ddCU6cwbFAipf',
  'aud': 'humidity',
  'scope': ['sub'],
  'cnf': {
    'jwk': {
      'alg': 'HS256',
      'kty': 'oct',
      'k': 'T1po1u9bxuI6G+AfABj7uOT8XNmgr6YRt4+ahG4agINwCkGJ6w/4yOcwfxElOCLOoA0aWXLP4m+fLjXDteXP2+OYNIwYT5CPNsCrvVw1G9CgZKrxX8BhmXqb3URihO+Ukw4AKNX0hVfyvU++mQugMPjtJCCP+bdERT1S8o77318='
    }
  }
}
```


#### Step 6: Broker CONNACK

The broker disauthenticates the user for the shortened POP.

```
{
  reasonCode=PAYLOAD_FORMAT_INVALID, 
  sessionPresent=false
}
```

The broker disauthenticates the user for the invalid POP.
```
{
  reasonCode=NOT_AUTHORIZED, 
  sessionPresent=false
}
```





## MQTT Version 5 Testing

### Test case 1: Successful Authentication, CONNECT with complete Authentication Data
Examine the messages for a version 5 client who authenticates successfully by including both the access
token and the POP in the authentication data payload of the CONNECT message

#### Step 1: Client token request

```
INFO: HEADERS: REQUEST HEADERS:
POST /api/client/token HTTP/1.1
Content-Length: 66
Host: 127.0.0.1:3001
User-Agent: Java-http-client/12.0.1
Authorization: Basic ekUqZGRDVTZjd2JGQWlwZjo3Q3JHelN5emgxbC8yaXhSQzhYZm1WdFhXY0dEZjgrV3Vhbzh5YUlzWDF3PQ==
Content-Type: application/json

{
  "grant_type":"client_credentials",
  "scope":"sub",
  "aud":"humidity"
}
```

#### Step 2: AS server token response
```
INFO: RESPONSE: (POST http://127.0.0.1:3001/api/client/token) 200 HTTP_1_1 Local port:  56775
    cache-control: no-store
    connection: keep-alive
    content-type: application/json
    date: Thu, 28 Nov 2019 21:20:12 GMT
    pragma: no-cache
    set-cookie: connect.sid=s%3AOweoCZEKrUquW947hHgVBTHLJaXRiYll.9iTtigeG87HiQCnRrbpkWs%2B%2FLeDk9KYAhfM0hXW%2BrKw; Path=/; Expires=Thu, 03 Sep 2020 21:20:12 GMT; HttpOnly
    transfer-encoding: chunked
    x-powered-by: Express

{
  'access_token': '3a7exuu7xvpayk69vrc3tvz335h1tujhygtq2pnth4mfctmfhqrp0mqmkj6epehv9yhvxr1k87p061qmuxncgdah65d3yzb7d3faq6v2cry6d9dcv8456mrwxnkeq1mmegct9yk45a77y6kfzc6ce1teh58kgbz9v5tt0rtpdjth3pmv5hudf5bbbt3vwykzwj7hm9r2p0vakpf0x48wafwwgc006btd1gzzxduf744rzur6x6h8qx48hdfkf9kgj5wh563d1jmrpan7znth7g75w6zrp5cc2wtaa8p0dy247kybmua8c8qcdj6w3avqvqw67nm14y0y84czb4mevyxrrf9kyp178bpkjd7qwyxp1gfwafehbff5bxqrx1jdjy2d1xcmvd0hf4dc000kt6gbx0',
  'profile': 'mqtt_tls',
  'token_type': 'pop',
  'exp': 1575012012809,
  'cnf': {
    'jwk': {
      'kty': 'oct',
      'alg': 'HS512',
      'k': 'ZNPJwi4gc58qR6Jnyj0u3vZl2LZqEEkBM8YNJKZGytXHlEBj8mwT5pnRbAce3lJ63UjvDCugV3T5bzwDk918PVdvRZkh6xbwHBtVj9G/RMNzBlXULLAMDV3Bt8Ks4sAxLM3FIbK4YosCbCmVSHyjL0BVZilUe/X0++H/SSDTXZU='
    }
  }
}
```

#### Step 3: Client CONNECT 
Specified by [Section 3.1](https://art.tools.ietf.org/html/draft-sengul-ace-mqtt-tls-profile-04#section-3.1). The client send a CONNECT packet including the complete authentication data.
```
2019-11-28 21:20:13,215 INFO  - Received CONNECT from client 'zE*ddCU6cwbFAipf': 
    Protocol version: 'V_5', 
    Clean Start: 'true', 
    Session Expiry Interval: '0', 
    Keep Alive: '60', 
    Maximum Packet Size: '268435460', 
    Receive Maximum: '65535', 
    Topic Alias Maximum: '0', 
    Request Problem Information: 'true',
    Request Response Information: 'false',  
    Username: 'null', 
    Password: 'null', 
    Auth Method: 'ace', 
    Auth Data (Base64): 'AZozYTdleHV1N3h2cGF5azY5dnJjM3R2ejMzNWgxdHVqaHlndHEycG50aDRtZmN0bWZocXJwMG1xbWtqNmVwZWh2OXlodnhyMWs4N3AwNjFxbXV4bmNnZGFoNjVkM3l6YjdkM2ZhcTZ2MmNyeTZkOWRjdjg0NTZtcnd4bmtlcTFtbWVnY3Q5eWs0NWE3N3k2a2Z6YzZjZTF0ZWg1OGtnYno5djV0dDBydHBkanRoM3BtdjVodWRmNWJiYnQzdnd5a3p3ajdobTlyMnAwdmFrcGYweDQ4d2Fmd3dnYzAwNmJ0ZDFnenp4ZHVmNzQ0cnp1cjZ4Nmg4cXg0OGhkZmtmOWtnajV3aDU2M2Qxam1ycGFuN3pudGg3Zzc1dzZ6cnA1Y2Myd3RhYThwMGR5MjQ3a3libXVhOGM4cWNkajZ3M2F2cXZxdzY3bm0xNHkweTg0Y3piNG1ldnl4cnJmOWt5cDE3OGJwa2pkN3F3eXhwMWdmd2FmZWhiZmY1YnhxcngxamRqeTJkMXhjbXZkMGhmNGRjMDAwa3Q2Z2J4MABANkEwQzc3N0U5Njk2MjRBRDBCOTQ3NDY5MjUyQUVENjdBREI5MEU2QzlCMjA5OUQyQ0MwMzFCNjM1ODg3QTAxRA==', 
    User Properties: 'null'
```

Notes on standard compliance:

| Field                   	| Standard value                                                                                      	| Actual value     	| Notes                                     	|
|-------------------------	|-----------------------------------------------------------------------------------------------------	|------------------	|-------------------------------------------	|
| Version                 	| Set to V5                                                                                           	| Set to V5        	| ✓                                         	|
| Username flag           	| Set to 0                                                                                            	| Set to 0         	| ✓                                         	|
| Password flag           	| Set to 0                                                                                            	| Set to 0         	| ✓                                         	|
| Session Expiry Interval 	| Set to 0                                                                                            	| Set to 0         	| ✓                                         	|
| Clean start flag        	| Set to 1                                                                                            	| Set to 1         	| ✓                                         	|
| Authentication method   	| Set to 'ace'                                                                                        	| Set to 'ace'     	| ✓                                         	|
| Authentication payload  	| Include the Access Token as a UTF-8 length preceded string and POP as binary length preceded data. 	| Same as Standard 	| Need to check for CBOR and COSE encoding. 	|


#### Step 4: Broker token introspection request

```
2019-11-28 21:20:13,219 INFO  - HEADERS: REQUEST HEADERS:
POST /api/rs/introspect HTTP/1.1
Content-Length: 428
Host: 127.0.0.1:3001
User-Agent: Java-http-client/11.0.4
Authorization: Basic cCo2b3NvIWVJM0Qyd3NoSzpCQnlVd2I3L0ZpenNzRGNtSTBBR1Z0SVI4dnZ1WkpSMHBhN3NXRjdtRGR3PQ==
Content-Type: application/json

{
  "token" : "3a7exuu7xvpayk69vrc3tvz335h1tujhygtq2pnth4mfctmfhqrp0mqmkj6epehv9yhvxr1k87p061qmuxncgdah65d3yzb7d3faq6v2cry6d9dcv8456mrwxnkeq1mmegct9yk45a77y6kfzc6ce1teh58kgbz9v5tt0rtpdjth3pmv5hudf5bbbt3vwykzwj7hm9r2p0vakpf0x48wafwwgc006btd1gzzxduf744rzur6x6h8qx48hdfkf9kgj5wh563d1jmrpan7znth7g75w6zrp5cc2wtaa8p0dy247kybmua8c8qcdj6w3avqvqw67nm14y0y84czb4mevyxrrf9kyp178bpkjd7qwyxp1gfwafehbff5bxqrx1jdjy2d1xcmvd0hf4dc000kt6gbx0"
}
```
#### Step 5: AS Server token introspection response

```
2019-11-28 21:20:13,223 INFO  - RESPONSE: (POST http://127.0.0.1:3001/api/rs/introspect) 200 HTTP_1_1 Local port:  56777
    connection: keep-alive
    content-length: 332
    content-type: application/json; charset=utf-8
    date: Thu, 28 Nov 2019 21:20:13 GMT
    etag: W/"14c-3BN40y2iCpVT5HaAVPS4tAOc/vY"
    set-cookie: connect.sid=s%3AKIhWjPNEzifbtnqySecizpZSH2GZHbnD.ERskuCDOJ2cKDjl0dvKeNCGwO8JhlfwE2vgCcW3JuLg; Path=/; Expires=Thu, 03 Sep 2020 21:20:13 GMT; HttpOnly
    x-powered-by: Express

{
  'active': true,
  'profile': 'mqtt_tls',
  'exp': 1575012012,
  'sub': 'zE*ddCU6cwbFAipf',
  'aud': 'humidity',
  'scope': ['sub'],
  'cnf': {
    'jwk': {
      'alg': 'HS256',
      'kty': 'oct',
      'k': 'ZNPJwi4gc58qR6Jnyj0u3vZl2LZqEEkBM8YNJKZGytXHlEBj8mwT5pnRbAce3lJ63UjvDCugV3T5bzwDk918PVdvRZkh6xbwHBtVj9G/RMNzBlXULLAMDV3Bt8Ks4sAxLM3FIbK4YosCbCmVSHyjL0BVZilUe/X0++H/SSDTXZU='
    }
  }
}
```

#### Step 6: Broker CONNACK 
Specified by [Section 3.1](https://art.tools.ietf.org/html/draft-sengul-ace-mqtt-tls-profile-04#section-3.1). The broker confirmed that the access token is valid and not expired and that the POP is valid, thus authenticates the client
```
{
  reasonCode=SUCCESS, 
  sessionPresent=false, 
  enhancedAuth=MqttEnhancedAuth{method=ace}, 
  restrictions={receiveMaximum=10, maximumPacketSize=268435460, topicAliasMaximum=5, maximumQos=EXACTLY_ONCE, retainAvailable=true, wildcardSubscriptionAvailable=true, sharedSubscriptionAvailable=true, subscriptionIdentifiersAvailable=true}
}
```


### Test case 2: Successful Authentication, CONNECT using username and password

Examine the messages for a version 5 client who authenticates successfully by including both the access
token and the POP in the username and password fields of the CONNECT message, similarly to a version 3 client

#### Step 1: Client token request

```
POST /api/client/token HTTP/1.1
Content-Length: 66
Host: 127.0.0.1:3001
User-Agent: Java-http-client/12.0.1
Authorization: Basic ekUqZGRDVTZjd2JGQWlwZjo3Q3JHelN5emgxbC8yaXhSQzhYZm1WdFhXY0dEZjgrV3Vhbzh5YUlzWDF3PQ==
Content-Type: application/json

{
  "grant_type":"client_credentials",
  "scope":"sub",
  "aud":"humidity"
}
```

#### Step 2: AS server token response
```
INFO: RESPONSE: (POST http://127.0.0.1:3001/api/client/token) 200 HTTP_1_1 Local port:  62056
    cache-control: no-store
    connection: keep-alive
    content-type: application/json
    date: Sat, 30 Nov 2019 20:07:22 GMT
    pragma: no-cache
    set-cookie: connect.sid=s%3AzoGi0DvX-v7hjQQ7hhD9thaDp4lArBQs.Bzya1%2BF%2BPmQ%2FJ%2FMIbi3xzQwdcAips964AhBC9YVZUiU; Path=/; Expires=Sat, 05 Sep 2020 20:07:22 GMT; HttpOnly
    transfer-encoding: chunked
    x-powered-by: Express

{
  'access_token': 'mhwzj2jwmy8apuh3z4em9yg3ruumaw2gqzjtrwcuvxyuev3bb1cz9a90jzrq1zzte7x98z98097t73cnhv6bxd7hp22w485xy5vwmakqdukb3483g59fmr3w6bb0kq5wzzkwjcg1j6up87yw004ukcnbp5t0mc2j22g15d94fpydm6gjhjmfdawgq1z00xwzyhexh0bam0x1bn4ggxxub61pr8h7y0kxkrt0whyyzhxeuvtqpfx2pnzaartwczwpqbqwfyw1m4cjfmfr95fxr1dzej6z9ffjcgj3nxxaff7vdkwrtchguhffhjy2e77abrew3f6dua7rnddnzkv6cdqnu6nx0jctmzq74f4rfu4jeuw9y61kaj8tz6qykau2dfy3wp667gq587kwwgfyj0ncjr',
  'profile': 'mqtt_tls',
  'token_type': 'pop',
  'exp': 1575180442213,
  'cnf': {
    'jwk': {
      'kty': 'oct',
      'alg': 'HS512',
      'k': 'pr4dmvBn2ADFq0ftX1UpfTarS/EBUVDSd9U9fuE7lToWM0ca7Rmx+XpmU7yT3KSsUe8NjmTR5DlayXnWIKd8GzSmmHJ7zIoVy1DKtl6OHDZDaPdMZ+vNCjB4NEP+18YqDV8S7Cf/xRnNG0LatKHU2PVs3WgvJb80/2T8Xb139aM='
    }
  }
}
```
#### Step 3: Client CONNECT

Partly specified by [Section 3.1](https://art.tools.ietf.org/html/draft-sengul-ace-mqtt-tls-profile-04#section-3.1). The client send a CONNECT packet with username being the access token string and password being the POP.

```
2019-11-30 20:07:22,681 INFO  - Received CONNECT from client 'zE*ddCU6cwbFAipf': 
    Protocol version: 'V_5', 
    Clean Start: 'true', 
    Session Expiry Interval: '0', 
    Keep Alive: '60', 
    Maximum Packet Size: '268435460', 
    Receive Maximum: '65535', 
    Topic Alias Maximum: '0', 
    Request Problem Information: 'true', 
    Request Response Information: 'false',  
    Username: 'mhwzj2jwmy8apuh3z4em9yg3ruumaw2gqzjtrwcuvxyuev3bb1cz9a90jzrq1zzte7x98z98097t73cnhv6bxd7hp22w485xy5vwmakqdukb3483g59fmr3w6bb0kq5wzzkwjcg1j6up87yw004ukcnbp5t0mc2j22g15d94fpydm6gjhjmfdawgq1z00xwzyhexh0bam0x1bn4ggxxub61pr8h7y0kxkrt0whyyzhxeuvtqpfx2pnzaartwczwpqbqwfyw1m4cjfmfr95fxr1dzej6z9ffjcgj3nxxaff7vdkwrtchguhffhjy2e77abrew3f6dua7rnddnzkv6cdqnu6nx0jctmzq74f4rfu4jeuw9y61kaj8tz6qykau2dfy3wp667gq587kwwgfyj0ncjr', 
    Password (Hex): '004043373739453330413744423735313534383544444141304134314444383544394439383038433637354138323336453146384641374634324142423146423736', 
    Auth Method: 'ace', 
    Auth Data (Base64): 'null', 
    User Properties: 'null'
```

Notes on standard compliance:

| Field                   | Standard value                                                                               | Actual value                              | Notes                                                                       |
|-------------------------|----------------------------------------------------------------------------------------------|-------------------------------------------|-----------------------------------------------------------------------------|
| Version                 | Set to V5                                                                                    | Set to V5                                 | ✓                                                                           |
| Username flag           | Set to 1                                                                                     | Set to 1                                  | Indicated by the non null username field                                    |
| Password flag           | Set to 1                                                                                     | Set to 1                                  | Indicated by the non null password field                                    |
| Username                | Set to access token string, UTF-8 encoded                                                    | Set to access token string, UTF-8 encoded | ✓                                                                           |
| Password                | Set to MAC/DiSig over entire packet or a  nonce included in the password field along the POP | Set to MAC over the access token string   | Simplified use case. Currently only support for  MAC over predefined string |
| Session Expiry Interval | Set to 0                                                                                     | Set to 0                                  | ✓                                                                           |
| Clean start flag        | Set to 1                                                                                     | Set to 1                                  | ✓                                                                           |
| Authentication method   | Set to 'ace'                                                                                 | Set to 'ace'                              | This is not explicitly specified                                            |
| Authentication payload  | Empty                                                                                        | Empty                                     | This is not explicitly specified                                            |

#### Step 4: Broker token introspection request
```
POST /api/rs/introspect HTTP/1.1
Content-Length: 428
Host: 127.0.0.1:3001
User-Agent: Java-http-client/11.0.4
Authorization: Basic cCo2b3NvIWVJM0Qyd3NoSzpCQnlVd2I3L0ZpenNzRGNtSTBBR1Z0SVI4dnZ1WkpSMHBhN3NXRjdtRGR3PQ==
Content-Type: application/json

{
  "token" : "mhwzj2jwmy8apuh3z4em9yg3ruumaw2gqzjtrwcuvxyuev3bb1cz9a90jzrq1zzte7x98z98097t73cnhv6bxd7hp22w485xy5vwmakqdukb3483g59fmr3w6bb0kq5wzzkwjcg1j6up87yw004ukcnbp5t0mc2j22g15d94fpydm6gjhjmfdawgq1z00xwzyhexh0bam0x1bn4ggxxub61pr8h7y0kxkrt0whyyzhxeuvtqpfx2pnzaartwczwpqbqwfyw1m4cjfmfr95fxr1dzej6z9ffjcgj3nxxaff7vdkwrtchguhffhjy2e77abrew3f6dua7rnddnzkv6cdqnu6nx0jctmzq74f4rfu4jeuw9y61kaj8tz6qykau2dfy3wp667gq587kwwgfyj0ncjr"
}
```
#### Step 5: AS Server token introspection response
```
2019-11-30 20:07:23,089 INFO  - RESPONSE: (POST http://127.0.0.1:3001/api/rs/introspect) 200 HTTP_1_1 Local port:  62058
    connection: keep-alive
    content-length: 332
    content-type: application/json; charset=utf-8
    date: Sat, 30 Nov 2019 20:07:23 GMT
    etag: W/"14c-tNJNZqmVcCSTDdpOa9xWvDjy2QM"
    set-cookie: connect.sid=s%3ASG4FsyS-yKloTGuUmUkU2nElD53H07YP.G2bZPrNZxjabsd9z7Wy46EV9OXge7eo3nlDjHZ2Nxjc; Path=/; Expires=Sat, 05 Sep 2020 20:07:23 GMT; HttpOnly
    x-powered-by: Express

{
  'active': true,
  'profile': 'mqtt_tls',
  'exp': 1575180442,
  'sub': 'zE*ddCU6cwbFAipf',
  'aud': 'humidity',
  'scope': ['sub'],
  'cnf': {
    'jwk': {
      'alg': 'HS256',
      'kty': 'oct',
      'k': 'pr4dmvBn2ADFq0ftX1UpfTarS/EBUVDSd9U9fuE7lToWM0ca7Rmx+XpmU7yT3KSsUe8NjmTR5DlayXnWIKd8GzSmmHJ7zIoVy1DKtl6OHDZDaPdMZ+vNCjB4NEP+18YqDV8S7Cf/xRnNG0LatKHU2PVs3WgvJb80/2T8Xb139aM='
    }
  }
}
```

#### Step 6: Broker CONNACK
The broker accepts the connection.

```
{
  reasonCode=SUCCESS, 
  sessionPresent=false, 
  enhancedAuth=MqttEnhancedAuth{method=ace}, 
  restrictions=MqttConnAckRestrictions{receiveMaximum=10, maximumPacketSize=268435460, topicAliasMaximum=5, maximumQos=EXACTLY_ONCE, retainAvailable=true, wildcardSubscriptionAvailable=true, sharedSubscriptionAvailable=true, subscriptionIdentifiersAvailable=true}
}
```

Notes on standard compliance:
- It is not specified whether username & password field authentication has lower precedence than authentication data
thus at the moment the broker checks for the former only in case the latter is empty.


### Test case 3: Successful Authentication, Challenge AUTH

Examine the messages for a version 5 client who authenticates successfully after passing challenge 
authentication, by including only the access token string in the authentication data CONNECT packet.

#### Step 1: Client token request

```
POST /api/client/token HTTP/1.1
Content-Length: 66
Host: 127.0.0.1:3001
User-Agent: Java-http-client/12.0.1
Authorization: Basic ekUqZGRDVTZjd2JGQWlwZjo3Q3JHelN5emgxbC8yaXhSQzhYZm1WdFhXY0dEZjgrV3Vhbzh5YUlzWDF3PQ==
Content-Type: application/json

{
  'grant_type': 'client_credentials', 
  'scope': 'sub', 
  'aud': 'humidity'
}
```

#### Step 2: AS server token response

```
INFO: RESPONSE: (POST http://127.0.0.1:3001/api/client/token) 200 HTTP_1_1 Local port:  62843
    cache-control: no-store
    connection: keep-alive
    content-type: application/json
    date: Sat, 30 Nov 2019 22:00:08 GMT
    pragma: no-cache
    set-cookie: connect.sid=s%3ABwZJAfr7b2U2MiQrHnsU7qDEwize5CY-.behFrLE6cX3ps9sdFdE8%2BihZfEcQXmt5O3BfW0ek%2FyY; Path=/; Expires=Sat, 05 Sep 2020 22:00:08 GMT; HttpOnly
    transfer-encoding: chunked
    x-powered-by: Express

{
  'access_token': 'kr37u4gxpbyk7tuq3a7d6wa6wktyu83me3pv1rvpv3xy22qdvuqj9394w05h93wh69jr6c20uy8vvwjvgkprraemdw95xzaq0hd04tq857mab1z3p2grnm4xnut732yq9vy46rq3puvrnx0v2wrcxr5v6tpp9a0d210bzuhe846etdxrqn37gb1fuxmndee2d7tydrtaq0urqta51xwzcpyxmka8knauv94chxdgeag4ubnzvrxxep2y791m4uf8c2kfxy9eubyb4m6zz3tz88qvkp5f4kvnu5wgaunv5f4phe1y613f9yvmbe7nqhkkzkx503xja403pnte2gpv3mbf60e398m79jukm7y9qaqfer52c1kxv0xbvzvm9z880d9vdfwvegzu0zqb2ewry3bj70',
  'profile': 'mqtt_tls',
  'token_type': 'pop',
  'exp': 1575187208023,
  'cnf': {
    'jwk': {
      'kty': 'oct',
      'alg': 'HS512',
      'k': 't2PH4n9s4geIE3d212FJQ6DFNT/KIh90ZFW6yk1VpgYpwFaxo8OyByPgCEc+eop2QKT+lFNMob36RZkqoNtGmb//6GX+i94o/zMKwOyBMV1QGiR4LwFt6yZJRfmyNS5DTMHm8VDUN5QloYDWpgkys/5CbF5BBCWhaDYjpERzoCc='
    }
  }
}
```

#### Step 3: Client CONNECT

Specified by [Section 3.1](https://art.tools.ietf.org/html/draft-sengul-ace-mqtt-tls-profile-04#section-3.1). The client sends a CONNECT message with authentication data including only the access token string.
```
2019-11-30 22:00:08,388 INFO  - Received CONNECT from client 'zE*ddCU6cwbFAipf': 
    Protocol version: 'V_5', 
    Clean Start: 'true', 
    Session Expiry Interval: '0', 
    Keep Alive: '60', 
    Maximum Packet Size: '268435460', 
    Receive Maximum: '65535', 
    Topic Alias Maximum: '0', 
    Request Problem Information: 'true', 
    Request Response Information: 'false',  
    Username: 'null', 
    Password: 'null', 
    Auth Method: 'ace', 
    Auth Data (Base64): 'AZprcjM3dTRneHBieWs3dHVxM2E3ZDZ3YTZ3a3R5dTgzbWUzcHYxcnZwdjN4eTIycWR2dXFqOTM5NHcwNWg5M3doNjlqcjZjMjB1eTh2dndqdmdrcHJyYWVtZHc5NXh6YXEwaGQwNHRxODU3bWFiMXozcDJncm5tNHhudXQ3MzJ5cTl2eTQ2cnEzcHV2cm54MHYyd3JjeHI1djZ0cHA5YTBkMjEwYnp1aGU4NDZldGR4cnFuMzdnYjFmdXhtbmRlZTJkN3R5ZHJ0YXEwdXJxdGE1MXh3emNweXhta2E4a25hdXY5NGNoeGRnZWFnNHVibnp2cnh4ZXAyeTc5MW00dWY4YzJrZnh5OWV1YnliNG02enozdHo4OHF2a3A1ZjRrdm51NXdnYXVudjVmNHBoZTF5NjEzZjl5dm1iZTducWhra3preDUwM3hqYTQwM3BudGUyZ3B2M21iZjYwZTM5OG03OWp1a203eTlxYXFmZXI1MmMxa3h2MHhidnp2bTl6ODgwZDl2ZGZ3dmVnenUwenFiMmV3cnkzYmo3MA==', 
    User Properties: 'null'
```

Notes on standard compliance:

| Field                   	| Standard value                                                                                      	| Actual value     	| Notes                                     	|
|-------------------------	|-----------------------------------------------------------------------------------------------------	|------------------	|-------------------------------------------	|
| Version                 	| Set to V5                                                                                           	| Set to V5        	| ✓                                         	|
| Username flag           	| Set to 0                                                                                            	| Set to 0         	| Indicated by null value on username field     |
| Password flag           	| Set to 0                                                                                            	| Set to 0         	| Indicated by null value on password field     |
| Session Expiry Interval 	| Set to 0                                                                                            	| Set to 0         	| ✓                                         	|
| Clean start flag        	| Set to 1                                                                                            	| Set to 1         	| ✓                                         	|
| Authentication method   	| Set to 'ace'                                                                                        	| Set to 'ace'     	| ✓                                         	|
| Authentication payload  	| Include the Access Token as a UTF-8 length preceded string                                        	| Same as Standard 	| Need to check for CBOR and COSE encoding. 	|



#### Step 4: Broker token introspection request
The broker introspects the access token string normally.
```
POST /api/rs/introspect HTTP/1.1
Content-Length: 428
Host: 127.0.0.1:3001
User-Agent: Java-http-client/11.0.4
Authorization: Basic cCo2b3NvIWVJM0Qyd3NoSzpCQnlVd2I3L0ZpenNzRGNtSTBBR1Z0SVI4dnZ1WkpSMHBhN3NXRjdtRGR3PQ==
Content-Type: application/json

{
  "token" : "kr37u4gxpbyk7tuq3a7d6wa6wktyu83me3pv1rvpv3xy22qdvuqj9394w05h93wh69jr6c20uy8vvwjvgkprraemdw95xzaq0hd04tq857mab1z3p2grnm4xnut732yq9vy46rq3puvrnx0v2wrcxr5v6tpp9a0d210bzuhe846etdxrqn37gb1fuxmndee2d7tydrtaq0urqta51xwzcpyxmka8knauv94chxdgeag4ubnzvrxxep2y791m4uf8c2kfxy9eubyb4m6zz3tz88qvkp5f4kvnu5wgaunv5f4phe1y613f9yvmbe7nqhkkzkx503xja403pnte2gpv3mbf60e398m79jukm7y9qaqfer52c1kxv0xbvzvm9z880d9vdfwvegzu0zqb2ewry3bj70"
}
```

#### Step 5: AS Server token introspection response

```
2019-11-30 22:00:08,401 INFO  - RESPONSE: (POST http://127.0.0.1:3001/api/rs/introspect) 200 HTTP_1_1 Local port:  62845
    connection: keep-alive
    content-length: 332
    content-type: application/json; charset=utf-8
    date: Sat, 30 Nov 2019 22:00:08 GMT
    etag: W/"14c-yv4Ql0wI0HJqMROXBjcqzamDkmU"
    set-cookie: connect.sid=s%3AluObMjQS7zYvFrBAvFIhGa7IqxHEvl95.xHOq6p0NxeK3IhXuwTK%2BpqLwruHkeEiZoiiVCCsMoe4; Path=/; Expires=Sat, 05 Sep 2020 22:00:08 GMT; HttpOnly
    x-powered-by: Express
    
{
  'active': true,
  'profile': 'mqtt_tls',
  'exp': 1575187208,
  'sub': 'zE*ddCU6cwbFAipf',
  'aud': 'humidity',
  'scope': ['sub'],
  'cnf': {
    'jwk': {
      'alg': 'HS256',
      'kty': 'oct',
      'k': 't2PH4n9s4geIE3d212FJQ6DFNT/KIh90ZFW6yk1VpgYpwFaxo8OyByPgCEc+eop2QKT+lFNMob36RZkqoNtGmb//6GX+i94o/zMKwOyBMV1QGiR4LwFt6yZJRfmyNS5DTMHm8VDUN5QloYDWpgkys/5CbF5BBCWhaDYjpERzoCc='
    }
  }
}
```

#### Step 6: Broker AUTH
Specified by [Section 3.1](https://art.tools.ietf.org/html/draft-sengul-ace-mqtt-tls-profile-04#section-3.1). The broker verifies that the access token string is active and thus it proceeds to send a challenge AUTH packet.
```
{
reasonCode= CONTINUE_AUTHENTICATION, 
method=ace, 
data=00203E54178F52F93F823BB3A47049F27E583546B9CE3A43F878CA04A7BB7937687E, 
reasonString=null
}
```

Notes on standard compliance:

| Field                 	| Standard value                 	| Actual value                      	| Notes                                                                             	|
|-----------------------	|--------------------------------	|-----------------------------------	|-----------------------------------------------------------------------------------	|
| Reason Code           	| Set to CONTINUE_AUTHENTICATION 	| Set to CONTINUE_AUTHENTICATION    	| ✓                                                                                 	|
| Authentication Method 	| Set to 'ace'                   	| Set to 'ace'                      	| ✓                                                                                 	|
| Authentication Data   	| Set to a challenge             	| Set to a random 32 byte challenge 	| Length and origin of nonce unspecified so used the Random java class and 32 bytes 	|
| Authentication method 	| Set to 'ace'                   	| Set to 'ace'                      	| ✓                                                                                 	|
| Reason String         	| Unspecified                    	| Null                              	| ✓                                                                                 	|


#### Step 7: Client AUTH

Specified by [Section 3.1](https://art.tools.ietf.org/html/draft-sengul-ace-mqtt-tls-profile-04#section-3.1). The client computes the POP over the received challenge and responds with an AUTH packet.
```
{
reasonCode= CONTINUE_AUTHENTICATION, 
method=ace, 
data=004036393246354436413046354131414444443333454339463036384435363637334635343841453134313931393441364338314244373241443044333446324332, 
reasonString=null
}
```

Notes on standard compliance:

| Field                 	| Standard value                 	| Actual value                   	| Notes                    	|
|-----------------------	|--------------------------------	|--------------------------------	|--------------------------	|
| Reason Code           	| Set to CONTINUE_AUTHENTICATION 	| Set to CONTINUE_AUTHENTICATION 	| ✓                        	|
| Authentication Method 	| Set to 'ace'                   	| Set to 'ace'                   	| ✓                        	|
| Authentication Data   	| DiSig / MAC over challenge     	| MAC over challenge             	| No support for DiSig yet 	|
| Authentication method 	| Set to 'ace'                   	| Set to 'ace'                   	| ✓                        	|
| Reason String         	| Unspecified                    	| Null                           	| ✓                        	|

#### Step 8: Broker CONNACK
The broker verifies the POP and authenticates the client
```
{
  reasonCode=SUCCESS, 
  sessionPresent=false, 
  enhancedAuth=MqttEnhancedAuth{method=ace}, restrictions=MqttConnAckRestrictions{receiveMaximum=10, maximumPacketSize=268435460, topicAliasMaximum=5, maximumQos=EXACTLY_ONCE, retainAvailable=true, wildcardSubscriptionAvailable=true, sharedSubscriptionAvailable=true, subscriptionIdentifiersAvailable=true}
}
```

### Test case 4: Unsuccessful Authentication, AS Server discovery (Missing token and POP)
Examine the messages for a version 5 client who sends a CONNECT packet with empty payload data.

#### Step 1: Client CONNECT 
Specified by [Section 3.1](https://art.tools.ietf.org/html/draft-sengul-ace-mqtt-tls-profile-04#section-3.1). The client, probably unaware of the AS server IP address, sends a CONNECT packet with empty authentication data.
```
2019-11-28 21:02:01,041 INFO  - Received CONNECT from client 'zE*ddCU6cwbFAipf': 
    Protocol version: 'V_5', 
    Clean Start: 'true', 
    Session Expiry Interval: '0', 
    Keep Alive: '60', 
    Maximum Packet Size: '268435460', 
    Receive Maximum: '65535', 
    Topic Alias Maximum: '0', 
    Request Problem Information: 'true', 
    Request Response Information: 'false',  
    Username: 'null', 
    Password: 'null', 
    Auth Method: 'ace', 
    Auth Data (Base64): 'null', 
    User Properties: 'null'
```

Notes on standard compliance:

| Field                   	| Standard value 	| Actual value 	|
|-------------------------	|----------------	|--------------	|
| Version                 	| Set to V5      	| Set to V5    	|
| Username flag           	| Set to 0       	| Set to 0     	|
| Password flag           	| Set to 0       	| Set to 0     	|
| Session Expiry Interval 	| Set to 0       	| Set to 0     	|
| Clean start flag        	| Set to 1       	| Set to 1     	|
| Authentication method   	| Set to 'ace'   	| Set to 'ace' 	|
| Authentication payload  	| Empty          	| Empty        	|


#### Step 2: Broker CONNACK 
Specified by [Section 3.1](https://art.tools.ietf.org/html/draft-sengul-ace-mqtt-tls-profile-04#section-3.1). The broker disauthenticates the client and replies with 
```
{
  reasonCode=NOT_AUTHORIZED, 
  sessionPresent=false, 
  reasonString=Authentication token from provided server expected., 
  userProperties=[(AS, 127.0.0.1)]}
```

Notes on standard compliance:
- The broker responds with NOT_AUTHORIZED as specified.
- The AS server IP address is correctly sent over with the CONNACK but the name of the property 'AS' is chosen by me at the moment
- The cnonce property which could also be used in not implemented yet.



### Test case 5: Unsuccessful Authentication, CONNECT with Wrong Token

#### Step 1: Client token request
```
INFO: HEADERS: REQUEST HEADERS:
POST /api/client/token HTTP/1.1
Content-Length: 66
Host: 127.0.0.1:3001
User-Agent: Java-http-client/12.0.1
Authorization: Basic ekUqZGRDVTZjd2JGQWlwZjo3Q3JHelN5emgxbC8yaXhSQzhYZm1WdFhXY0dEZjgrV3Vhbzh5YUlzWDF3PQ==
Content-Type: application/json

{
  "grant_type":"client_credentials",
  "scope":"sub",
  "aud":"humidity"
}
```

#### Step 2: AS server token response
```
INFO: RESPONSE: (POST http://127.0.0.1:3001/api/client/token) 200 HTTP_1_1 Local port:  56925
    cache-control: no-store
    connection: keep-alive
    content-type: application/json
    date: Thu, 28 Nov 2019 21:45:41 GMT
    pragma: no-cache
    set-cookie: connect.sid=s%3A5o7DhJi4WDAMDcAJLEArm9-4Z-lKPO8n.SQA02cxFVxzRtfMbItwFwZuS0%2F0eRFivde3OFk1i8uA; Path=/; Expires=Thu, 03 Sep 2020 21:45:41 GMT; HttpOnly
    transfer-encoding: chunked
    x-powered-by: Express
    
{
  'access_token': '88y20mtt09yj4bx5dc17v48hf25urnjamub1fcgnzc6dmfw08p7mx4a10e5k2qxxdp4knghkrx81wdacuc7m7aa8786tuzp4tqc7k2jfywnmdbmggw51a7pq7acmz31qnxy7at1ykew192058dbdgtedrmr7hqbwpdwhz43v1ydw0t3byvkdgkuezw7vj7dc35n92a849y0wg3t6ct7h5ma6jbmt8jm5y4rwb3ubu3dtxd35hn2p3tx2h0e6zdh1vptng1x323txt8utn4u8r244j30byr9veuke9yj8qepg37xeqnygumymfrb5dxrkcp3193n2kfgfgv0jtpq65vpwy7g56xa0zabp43uabdntahtm28v0u8fk9gx1pd1ckctp807n0dux2vpg6ugwq8vtbm',
  'profile': 'mqtt_tls',
  'token_type': 'pop',
  'exp': 1575013541886,
  'cnf': {
    'jwk': {
      'kty': 'oct',
      'alg': 'HS512',
      'k': 'oZOhAo4nXUJpcauxL9MEUc3zJ7GVkyT+DdQbzNlp6CBQLX0/HArBmy0Pxb4+5JSvziYJKdKz6k9FgCmZyoEHdF35ImWpuJk8kZumPPN8MpHD0z4CZkhSpnli1Ns0hnP6/RpD/RHj9LhklArX47bLuF8QuoWzkCc/LgmcD7H/07s='
    }
  }
}
```

#### Step 3: Client CONNECT
Appended a letter 'a' at the end of the valid access token
```
2019-11-28 21:45:43,842 INFO  - Received CONNECT from client 'zE*ddCU6cwbFAipf': 
    Protocol version: 'V_5', 
    Clean Start: 'true',    
    Session Expiry Interval: '0', 
    Keep Alive: '60', 
    Maximum Packet Size: '268435460', 
    Receive Maximum: '65535', 
    Topic Alias Maximum: '0', 
    Request Problem Information: 'true', 
    Request Response Information: 'false',  
    Username: 'null', 
    Password: 'null', 
    Auth Method: 'ace', 
    Auth Data (Base64): 'AZs4OHkyMG10dDA5eWo0Yng1ZGMxN3Y0OGhmMjV1cm5qYW11YjFmY2duemM2ZG1mdzA4cDdteDRhMTBlNWsycXh4ZHA0a25naGtyeDgxd2RhY3VjN203YWE4Nzg2dHV6cDR0cWM3azJqZnl3bm1kYm1nZ3c1MWE3cHE3YWNtejMxcW54eTdhdDF5a2V3MTkyMDU4ZGJkZ3RlZHJtcjdocWJ3cGR3aHo0M3YxeWR3MHQzYnl2a2Rna3Vlenc3dmo3ZGMzNW45MmE4NDl5MHdnM3Q2Y3Q3aDVtYTZqYm10OGptNXk0cndiM3VidTNkdHhkMzVobjJwM3R4MmgwZTZ6ZGgxdnB0bmcxeDMyM3R4dDh1dG40dThyMjQ0ajMwYnlyOXZldWtlOXlqOHFlcGczN3hlcW55Z3VteW1mcmI1ZHhya2NwMzE5M24ya2ZnZmd2MGp0cHE2NXZwd3k3ZzU2eGEwemFicDQzdWFiZG50YWh0bTI4djB1OGZrOWd4MXBkMWNrY3RwODA3bjBkdXgydnBnNnVnd3E4dnRibWEAQDI4RkU4ODczREQxODNBOTU1MEY2NTE1REQxNEJGQjg0OUJBMjZBQkIyQTU0M0UwMjRDMEZGOEFFQkI3QUY5MjE=', 
    User Properties: 'null'
```

#### Step 4: Broker token introspection request
```
POST /api/rs/introspect HTTP/1.1
Content-Length: 429
Host: 127.0.0.1:3001
User-Agent: Java-http-client/11.0.4
Authorization: Basic cCo2b3NvIWVJM0Qyd3NoSzpCQnlVd2I3L0ZpenNzRGNtSTBBR1Z0SVI4dnZ1WkpSMHBhN3NXRjdtRGR3PQ==
Content-Type: application/json

{
  "token" : "88y20mtt09yj4bx5dc17v48hf25urnjamub1fcgnzc6dmfw08p7mx4a10e5k2qxxdp4knghkrx81wdacuc7m7aa8786tuzp4tqc7k2jfywnmdbmggw51a7pq7acmz31qnxy7at1ykew192058dbdgtedrmr7hqbwpdwhz43v1ydw0t3byvkdgkuezw7vj7dc35n92a849y0wg3t6ct7h5ma6jbmt8jm5y4rwb3ubu3dtxd35hn2p3tx2h0e6zdh1vptng1x323txt8utn4u8r244j30byr9veuke9yj8qepg37xeqnygumymfrb5dxrkcp3193n2kfgfgv0jtpq65vpwy7g56xa0zabp43uabdntahtm28v0u8fk9gx1pd1ckctp807n0dux2vpg6ugwq8vtbma"
}
```
#### Step 5: AS Server token introspection response
```
2019-11-28 21:45:43,850 INFO  - RESPONSE: (POST http://127.0.0.1:3001/api/rs/introspect) 200 HTTP_1_1 Local port:  56927
    connection: keep-alive
    content-length: 16
    content-type: application/json; charset=utf-8
    date: Thu, 28 Nov 2019 21:45:43 GMT
    etag: W/"10-iZ1Wee3XJp8Edii8tnDHQrctT0c"
    set-cookie: connect.sid=s%3A4mBbQyYBN5ukIYaaKPmQAGttVafEZloO.1I38xRJiWJWztqud5fRw4Ub1EFAtZzCmmScWDh0xeeQ; Path=/; Expires=Thu, 03 Sep 2020 21:45:43 GMT; HttpOnly
    x-powered-by: Express

{
  "active":false
}
```
#### Step 6: Broker CONNACK
```
{
  reasonCode=BAD_USER_NAME_OR_PASSWORD,
  sessionPresent=false,
  reasonString=Token expired.
}
```


### Test case 6: Unsuccessful Authentication, CONNECT with Wrong POP

#### Step 1: Client token request
```
POST /api/client/token HTTP/1.1
Content-Length: 66
Host: 127.0.0.1:3001
User-Agent: Java-http-client/12.0.1
Authorization: Basic ekUqZGRDVTZjd2JGQWlwZjo3Q3JHelN5emgxbC8yaXhSQzhYZm1WdFhXY0dEZjgrV3Vhbzh5YUlzWDF3PQ==
Content-Type: application/json

{
  "grant_type":"client_credentials",
  "scope":"sub",
  "aud":"humidity"
}
```
#### Step 2: AS server token response
```
INFO: RESPONSE: (POST http://127.0.0.1:3001/api/client/token) 200 HTTP_1_1 Local port:  57154
    cache-control: no-store
    connection: keep-alive
    content-type: application/json
    date: Thu, 28 Nov 2019 21:58:42 GMT
    pragma: no-cache
    set-cookie: connect.sid=s%3AG4fkmgqpMTkzkD75DKE7xY6yjMKpdoeR.902lf9foi%2BUq3P70iett0%2FopLDFfgK%2FPdyNHMnKCGrk; Path=/; Expires=Thu, 03 Sep 2020 21:58:42 GMT; HttpOnly
    transfer-encoding: chunked
    x-powered-by: Express

{
  'access_token': 'ge4u1pgrbd932eafpp3jd5z0qhk8wagv18n1uejzbrr9wjq6t7bfbjjfrc7kfh0zuk7t3m958t44qzdd6xrv49d5zy8prgd4r8qn21jw9t61h4ud3v0jzgva30f6avpa2ha0d831hbhq4nwa57f3v34v9jcrnv06fj8pc52awamxtdfmb1ee6q0huy6m9a5r2wum7n0hk57xt3bew93e2uhww644m1vjgkn1heb5dt5whxyhxa2haz26vxkr23ympm035chx3d5pda1kutxqybkvxx5808p0wvtw4bn56fwtfdbyp369n6j5q6hk4c1rvd5qav7r4tcaaq9jf8zt069j8en7m3xv0tz40zu6efc2rfef3dw96c0vha6jpa2bvhpydpbpjw52wzzt019mpcpwgw',
  'profile': 'mqtt_tls',
  'token_type': 'pop',
  'exp': 1575014322480,
  'cnf': {
    'jwk': {
      'kty': 'oct',
      'alg': 'HS512',
      'k': 'Nq6IRmzHFsMSJOL3mfj9KB8N39pKq1MzRElyUYq2PdmHazIg8pJkoomGg6z++oDBxyBslh/OQZAykBOw/i+8dF2gD8zRWqoQzP6nm3y10/KLTQGcBYTeQKHk6SlXQgwuF9fEt7qg5FmBsvatJ4umR2mhZXS1kSSiIvY5KBsda0A='
    }
  }
}
```
#### Step 3A: Client CONNECT with Invalid format POP, Different size than specified
Took the original array containing the MAC and shortened it to 40 bytes, leaving the specified size as it was originally
```
2019-11-28 22:07:21,877 INFO  - Received CONNECT from client 'zE*ddCU6cwbFAipf': 
    Protocol version: 'V_5', 
    Clean Start: 'true', 
    Session Expiry Interval: '0', 
    Keep Alive: '60', 
    Maximum Packet Size: '268435460', 
    Receive Maximum: '65535', 
    Topic Alias Maximum: '0', 
    Request Problem Information: 'true', 
    Request Response Information: 'false',  
    Username: 'null', 
    Password: 'null', 
    Auth Method: 'ace', 
    Auth Data (Base64): 'AZpyNWVtYzV4ZG1mYTEyN3JxcnVybjd0ZTR5em1yeDdjeGJnNmc2cHE2a3BuMmNwZGNicHA4NGpiOG55ODhhY21xYXIzdThrbXFtamdhdzVjd2FnYnJqYWI3NzAzNDJqemtncHR2dWs5cTRrdTA1OGp5bm00eWV1NmQzdnVjeDF3YnU5NXIzcnJwdGRqeGFhZXc4YWY2MTluMHFqZzRqYWd6NW5qeWF1cjdhZDBteGh5anhjNnlyYTltYzZ2YndhZ3U4ZmFwcm45dXJoNWd3NXo2cXZoOHJidGs1YXUzZWFoNnF6YXE1NWd1eDlxeW5qbmU0ZGN6ZG15MXg2Z2pibWQ0cjB1YnlueW56dTAyM2VtMGNleDVmNGZnMDZocDFlM3FnZzllNjZ0Nzk5YmhxcWttM3Z1NXBwY3Fka3g0NnA5bWh0a3ZtdWowNm5ldDYyMTBxNTM4cTcwNWVwdWphdjJlbTJheTE5cGM5cTR4Zmc0eDdkMDQzOWViMndmOHA5djZoeTQyeTRia2QxbWpra2IzMWZkOGdwZGVjYwBARDcxMDMyM0Q4QTJFRjVCMzc0NjNCM0Y0MkMzMEE5MzIxNzkyOEQ0OAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==', 
    User Properties: 'null'
```
#### Step 3B: Client CONNECT with Invalid format POP, Miscalculated 
Changed a byte in the computed POP array
```
2019-11-28 21:58:42,874 INFO  - Received CONNECT from client 'zE*ddCU6cwbFAipf': 
    Protocol version: 'V_5', 
    Clean Start: 'true', 
    Session Expiry Interval: '0', 
    Keep Alive: '60', 
    Maximum Packet Size: '268435460', 
    Receive Maximum: '65535', 
    Topic Alias Maximum: '0', 
    Request Problem Information: 'true', 
    Request Response Information: 'false',  
    Username: 'null', 
    Password: 'null', 
    Auth Method: 'ace', 
    Auth Data (Base64): 'AZpnZTR1MXBncmJkOTMyZWFmcHAzamQ1ejBxaGs4d2FndjE4bjF1ZWp6YnJyOXdqcTZ0N2JmYmpqZnJjN2tmaDB6dWs3dDNtOTU4dDQ0cXpkZDZ4cnY0OWQ1enk4cHJnZDRyOHFuMjFqdzl0NjFoNHVkM3YwanpndmEzMGY2YXZwYTJoYTBkODMxaGJocTRud2E1N2YzdjM0djlqY3JudjA2Zmo4cGM1MmF3YW14dGRmbWIxZWU2cTBodXk2bTlhNXIyd3VtN24waGs1N3h0M2JldzkzZTJ1aHd3NjQ0bTF2amdrbjFoZWI1ZHQ1d2h4eWh4YTJoYXoyNnZ4a3IyM3ltcG0wMzVjaHgzZDVwZGExa3V0eHF5Ymt2eHg1ODA4cDB3dnR3NGJuNTZmd3RmZGJ5cDM2OW42ajVxNmhrNGMxcnZkNXFhdjdyNHRjYWFxOWpmOHp0MDY5ajhlbjdtM3h2MHR6NDB6dTZlZmMycmZlZjNkdzk2YzB2aGE2anBhMmJ2aHB5ZHBicGp3NTJ3enp0MDE5bXBjcHdndwBANzUwMzFCNThDM0MyRUJBRkUyMzZBRUYzQTJDNTdCQzI3NzFDOTIyOAAxOEE3MDlDMTM4NEFFQzFDRUExRkIwOQ==', 
    User Properties: 'null'
```

#### Step 4: Broker token introspection request

```
POST /api/rs/introspect HTTP/1.1
Content-Length: 428
Host: 127.0.0.1:3001
User-Agent: Java-http-client/11.0.4
Authorization: Basic cCo2b3NvIWVJM0Qyd3NoSzpCQnlVd2I3L0ZpenNzRGNtSTBBR1Z0SVI4dnZ1WkpSMHBhN3NXRjdtRGR3PQ==
Content-Type: application/json

{
  "token" : "ge4u1pgrbd932eafpp3jd5z0qhk8wagv18n1uejzbrr9wjq6t7bfbjjfrc7kfh0zuk7t3m958t44qzdd6xrv49d5zy8prgd4r8qn21jw9t61h4ud3v0jzgva30f6avpa2ha0d831hbhq4nwa57f3v34v9jcrnv06fj8pc52awamxtdfmb1ee6q0huy6m9a5r2wum7n0hk57xt3bew93e2uhww644m1vjgkn1heb5dt5whxyhxa2haz26vxkr23ympm035chx3d5pda1kutxqybkvxx5808p0wvtw4bn56fwtfdbyp369n6j5q6hk4c1rvd5qav7r4tcaaq9jf8zt069j8en7m3xv0tz40zu6efc2rfef3dw96c0vha6jpa2bvhpydpbpjw52wzzt019mpcpwgw"
}
```

#### Step 5: AS Server token introspection response
```
2019-11-28 21:58:42,882 INFO  - RESPONSE: (POST http://127.0.0.1:3001/api/rs/introspect) 200 HTTP_1_1 Local port:  57156
    connection: keep-alive
    content-length: 332
    content-type: application/json; charset=utf-8
    date: Thu, 28 Nov 2019 21:58:42 GMT
    etag: W/"14c-l6xBrkMcklLzQgXx8kRACv/36ZQ"
    set-cookie: connect.sid=s%3Am9Juxv-j8XOlyeqQEiGYZGHICqYFUodd.kJGHe3kbcfdIwC%2FSlK6BYqy0c2esVk1H2lC1rCjXBKw; Path=/; Expires=Thu, 03 Sep 2020 21:58:42 GMT; HttpOnly
    x-powered-by: Express
    
{
  'active': true,
  'profile': 'mqtt_tls',
  'exp': 1575014322,
  'sub': 'zE*ddCU6cwbFAipf',
  'aud': 'humidity',
  'scope': ['sub'],
  'cnf': {
    'jwk': {
      'alg': 'HS256',
      'kty': 'oct',
      'k': 'Nq6IRmzHFsMSJOL3mfj9KB8N39pKq1MzRElyUYq2PdmHazIg8pJkoomGg6z++oDBxyBslh/OQZAykBOw/i+8dF2gD8zRWqoQzP6nm3y10/KLTQGcBYTeQKHk6SlXQgwuF9fEt7qg5FmBsvatJ4umR2mhZXS1kSSiIvY5KBsda0A='
    }
  }
}
```

#### Step 6: Broker CONNACK

```
{
reasonCode=NOT_AUTHORIZED, 
sessionPresent=false, 
reasonString=Unable to proof possession of token
}
```
