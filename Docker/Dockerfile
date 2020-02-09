FROM hivemq/hivemq-ce:snapshot

COPY config/broker/config.xml /opt/hivemq/conf/config.xml
COPY /oauth-hivemq /opt/hivemq/extensions/oauth-hivemq
ENV BROKER_CONFIG_DIR /opt/config
ENV HIVEMQ_ALLOW_ALL_CLIENTS "true"
COPY config/broker /opt/config

#tls mqtt & mqtts
EXPOSE 8883
EXPOSE 1883

WORKDIR /opt/hivemq

ENTRYPOINT ["/opt/docker-entrypoint.sh"]
CMD ["/opt/hivemq/bin/run.sh"]
