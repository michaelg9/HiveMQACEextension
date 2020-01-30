image:https://travis-ci.org/michaelg9/HiveMQACEextension.svg["Build Status", link="https://travis-ci.org/michaelg9/HiveMQACEextension"]
image:https://codecov.io/gh/michaelg9/HiveMQACEextension/branch/master/graph/badge.svg["Coverage Status", link="https://codecov.io/gh/michaelg9/HiveMQACEextension"]

== HiveMQ extension for ACE
=== Overview
This HiveMQ extension implements the mqtt-tls profile for ACE (Authorization and Authentication in Constrained Environments) described https://art.tools.ietf.org/html/draft-sengul-ace-mqtt-tls-profile-04[here].

=== Dependencies
This extension has the following dependencies:

- A HiveMQ broker implementation that supports and exposes the MQTTv5 extended authentication features to extensions. There is a custom implementation https://github.com/michaelg9/hivemq-community-edition[here]
- A fully v5 compliant MQTT client that supports ACE, which can be found https://github.com/michaelg9/HiveACEclient[here]
- An ACE authorization server running OAuth2, which can be found https://github.com/nominetresearch/ace-mqtt-mosquitto[here]

=== Build and Run Instructions

1. Clone the three repositories above each into its own directory.
2. Start the AS server, following the instructions in its own README file
3. Register a client for the MQTT broker to introspect tokens. Take a note of the client id and client secret
4. Record them into the config.properties file located in src/main/resources, along with the AS server IP address and port
5. Set up the broker by cloning, building it and extracting the zip folder created under build/zip. For more information see the README file of that repository.
6. Modify the tag <hiveMQDir> in the pom.xml file of this repository to point to the unzipped folder of the step above.
7. Run the broker with the extension by running the command *mvn clean package -PRunWithHiveMQ*
