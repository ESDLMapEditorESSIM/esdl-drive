# Start with OL runtime.
FROM open-liberty

USER root
# Symlink servers directory for easier mounts.
RUN ln -s /opt/ol/wlp/usr/servers /servers
ADD --chown=1001:0 target/liberty/wlp/usr/servers /servers/
RUN chmod -R ug+rX  /servers/hub
ADD --chown=1001:0 target/hub.war /servers/hub/apps
USER 1001
# copy certificates
RUN mkdir -p /servers/hub/resources/security
COPY public.p12 /servers/hub/resources/security/
# Run the server script and start the defaultServer by default.
ENTRYPOINT ["/opt/ol/wlp/bin/server", "run"]
CMD ["hub"]