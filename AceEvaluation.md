# MQTT profile for ACE evaluation
The purpose of this document is to understand and reflect on some thoughts that have been brought up 
regarding the MQTT TLS profile for ACE draft and implementation.

- [How to generate a nonce in MqttV3](#how-to-generate-a-nonce-in-mqttv3)
  * [Idea 1: TLS exporter](#idea-1-tls-exporter)
  * [Idea 2: Authorization Server generates the nonce](#idea-2-authorization-server-generates-the-nonce)
  * [Idea 3: Client specific topic for nonce generation](#idea-3-client-specific-topic-for-nonce-generation)
- [How is ACE useful on top of TLS](#how-is-ace-useful-on-top-of-tls)
  * [Authentication phase using complete TLS certificates](#authentication-phase-using-complete-tls-certificates)
    + [Value of TLS](#value-of-tls)
    + [Value of ACE](#value-of-ace)
  * [Authentication phase using TLS with Raw Public Keys](#authentication-phase-using-tls-with-raw-public-keys)
  * [Authorization](#authorization)
    + [Value of TLS](#value-of-tls-1)
    + [Value of ACE](#value-of-ace-1)
  * [Publish and Subscribe message exchange](#publish-and-subscribe-message-exchange)
    + [Value of TLS](#value-of-tls-2)
    + [Value of ACE](#value-of-ace-2)
- [Evaluation](#evaluation)
  * [Worth considering](#worth-considering)


## How to generate a nonce in MqttV3
There is a problem on how the nonce that will be used for the Proof Of Possession will be generated.
This is easy for MQTTv5 because the authentication phase can have more steps than just a CONNECT and a CONNACK packet,
so the broker can generate the nonce as a second step and send it as an AUTH packet to the client,
who calculates the POP on it and responds back with another AUTH packet.  
The problem for v3 is that the broker has to either accept or reject the CONNECT packet of the client, so 
there's little room to communicate a nonce.

### Idea 1: TLS exporter
There exists an non greatly supported concept of exporting data from the TLS handshake to the application layer to be
reused. In the TLS handshake, there are a few quite "random" parts:  
1. Client random (plaintext, 32bytes: 4 bytes for time and 28 bytes random)
2. Server random (plaintext, 32bytes: 4 bytes for time and 28 bytes random)
3. Pre master secret (either encrypted with server's public key or computer separately, never transmitted in plaintext)
4. Other random parameters depending on the type of cipher used, like Diffie Hellman numbers.
5. Master secret key

Number 1&2: Their last 28 bytes are considered to have been generated pseudo randomly. They are transmitted in plaintext though.
Number 3 is secret and probably shouldn't be used. According to the TLS IETF standard (RFC5246) ```The pre_master_secret should be deleted from memory once the master_secret has been computed.```   
Number 4 can't be used because it's dependent on the chosen cipher for the session.  
Number 5 is random but it's probably not advised to use it, as the security of the session is as good as this is secret.

Pros:
- **Efficient:** Since the values are already generated and transmitted, there is zero added overhead from re-using them
- **Simple:** If the values can be extracted, then there is no need to think where to put the nonce in the MQTTv3 packet.

Cons:
- **Ties the implementation to the use of TLS**. This might be OK since we're implementing the TLS profile for ACE but it wouldn't be a solution otherwise, since it ties the implementation to the transport layer.
- **Widely unsupported.** From a quick look I had, there's not easy or clean way to access such values in Java for example, because the values are either private or deleted from memory for security. Would this be supported on an embedded device with a TLS library?
- **Security concerns;** Apart from the client and server randoms, the other values are meant to stay secret. Regarding the client and server randoms, are they random enough to avoid a replay attack?


### Idea 2: Authorization Server generates the nonce
The idea is that the AS will generate the random, and give it to the client along with the POP key, when a new token is requested.
Then, the broker can retrieve the random along with the introspection request from the AS (or maybe the client can send it along with the POP)

Pros:
- **No need to transfer the nonce in the MQTT packet.**
- **Easy to implement**

Cons:
- **Increased AS - client interaction / Expensive**: The nonce is not supposed to be reusable for different sessions.
Thus, if the client disconnects and wants to reconnect, it needs to request a new token from the AS just to refresh the nonce.

### Idea 3: Client specific topic for nonce generation
The idea is that the broker allows a partly authenticated client (CONNECT with token and no POP) to subscribe to a unique topic used 
for one to one communication between the client and the broker to exchange the nonce. For example, the process could be as follows:
1. A client initially connects to the broker, over TLS supplying only the access token string.
2. The broker verifies the token is active and issued by the Authorization server. 
2. The client is authenticated but the default permissions for such client is to allow to only subscribe and publish to a topic named '/nonces/client-id'. 
3. The '/nonces/' topic prefix is special in the sense that only the client with the corresponding id can subscribe or publish to such topic.
4. On subscribe, the broker or a bot client creates a new nonce and publishes it to that topic and then caches it.
5. The client calculates the POP over that nonce and publishes it to that topic.
6. The broker verifies the POP by retrieving the nonce communicated in that topic.
7. If the POP is valid, the client gains the authorization permissions indicated by the token, otherwise the client is disconnected.


Pros:
- **Efficient**: Less overhead than the regular authentication case.
- **Simple**: No need to transfer the POP in the CONNECTv3 message.
- **Easily supported** from different implementations.
- **Not DoS vulnerable(?)**. The broker needs to cache the nonces exchanged in the /nonces/ topic category so it could be DoSed if
any client could connect to it. However, only clients with a valid token can subscribe to it, thus it reduces the scope of the
vulnerability 

Cons:
- **Security concerns?** If an attacker manages to steal a valid token (but not the POP key) then they would be authenticated 
with the broker, however they shouldn't be able to do anything useful. Could that be a security hole?


## How is ACE useful on top of TLS
This section expands on what is the value of ACE or TLS in each phase of a client lifecycle:

### Authentication phase using complete TLS certificates
During the authentication phase, a TLS protected communication channel is created between the client and the broker. Then the client
presents their token and the POP proof to the broker.

#### Value of TLS

| Benefits                                                                                                                                                          | Disadvantages                                      |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------|
| **Data Confidentiality**; Protects against eavesdroppers, using strong encryption.                                                                                | **High overhead**; Expensive TLS handshake and use |
| **Data Integrity**; Protects against message tampering, using message digest.                                                                                     |                                                    |
| **Authenticates broker to client**; The client can verify if the broker is the trusted one or an impersonator, by checking if it possesses the broker private key |                                                    |
| **Could authenticate client to broker**; The broker could challenge the client to see if it possesses the appropriate private key;                                |                                                    |

#### Value of ACE

| Benefits                                                                                                                                                                    | Disadvantages                                                                                                                                                                        |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Authenticates client to broker**; The broker can verify if the client is a trusted one or an impersonator, by checking if it possesses a valid token and can pass the POP | **Depends on guarantees of external libraries (TLS)**; The authentication phase would leak the token unless it's encrypted externally (doesn't support application layer encryption) |
| **Authorization permissions discovery**; The broker can figure out the permissions associated with the client token                                                         |                                                                                                                                                                                      |
| **Lightweight**; There is only some extra overhead during the authentication phase, but in general it doesn't require expensive operations                                  |                                                                                                                                                                                      |

### Authentication phase using TLS with Raw Public Keys
In order to decrease the overhead of the TLS handshake, we can also use RPK keys instead of complete certificates.
Additionally, if the deployment is using self signed certificates, then most of the information found on the certificate is useless.
In this case, ACE will be used to locate the public key of the broker, which will be specified in the token issues by the AS to the client.

### Authorization

#### Value of TLS

TLS has no role in MQTT client authorization.

#### Value of ACE

| Benefits                                                                                                                                                                                                  | Disadvantages                                                                                                                                                      |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Specifies client permissions**; The AS holds an ACL associated with the client token that specifies which resources can be accessed and how, and the broker can query the it any time                   | **Stale permissions(?)**; a broker that cached a token that was later revoked will still allow access to that client. This is remediated by token expiration life. |
| **Simple and lightweight**; The broker can just store the token on authentication and query it from memory any time the client attempts an action, which doesn't add any significant overhead to any side |                                                                                                                                                                    |
| **Allows modification of permissions on the fly**; by modifying / revoking client tokens on the AS                                                                                                        |                                                                                                                                                                    |

### Publish and Subscribe message exchange

#### Value of TLS
| Benefits                                                                                                                                                                   | Disadvantages                                   |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| **Message integrity**; Prevents modifying the contents of Subscribe or Publish messages                                                                                    | **Overhead**; Each message has to be encrypted  |
| **Guards from replay attacks**; There is no secret delivered with any message so an attacker knowing an authenticates client's id could replay packets or impersonate them |                                                 |
| **Message confidentiality**; Prevents anyone not subscribed to a topic knowing what's exchanged in that topic.                                                             |                                                 |

#### Value of ACE
There is no added security guarantees from ACE on message exchange

## Evaluation

ACE depends on external security guarantees for transport in order to provide its own guarantees. Thus, it's only 
usable in devices powerful enough to support TLS.  It would be nice if we could implement a scaled down version of it that has no requirement on TLS.

Regarding functionality, the authentication phase is mainly covered by TLS with ACE having none or auxiliary role if RPKs are used. 
However, ACE does include authentication features (POP, token) that could be useful if it's extended to a complete solution. However, currently it seems that there's some overlap of functions during the authentication phase.  
Regarding the authorization phase, ACE provides a flexible and lightweight way to control the permissions of the clients. 

### Worth considering
ACE has built in features that overlap some of those of TLS, that could be used to implement a less secure version of ACE
that has no dependency on TLS, to accommodate for device that don't support TLS or are not willing to take the overhead.  

The POP key is a pre shared secret between each client and the broker, that could be used for application layer encryption to
provide data confidentiality, or to provide data integrity through MAC digests. Also, if the token includes the broker's public key,
it can also be used to authenticate the broker to the client. 
