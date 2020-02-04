FROM hivemq/hivemq-ce:snapshot

COPY config.xml /opt/hivemq/conf/config.xml
COPY /oauth-hivemq /opt/hivemq/extensions/oauth-hivemq
COPY TLS /opt/TLS

#tls mqtt & mqtts
EXPOSE 8883
EXPOSE 1883

WORKDIR /opt/hivemq

ENTRYPOINT ["/opt/docker-entrypoint.sh"]
CMD ["/opt/hivemq/bin/run.sh"]
