# Note: this also copies a server configuration (including postgres libraries etc)
# So after a mvn package you need to do a mvn liberty:create
#
# Start with OL runtime.
FROM open-liberty:full-java17-openj9
#FROM icr.io/appcafe/open-liberty:full-java21-openj9-ubi-minimal

USER root
# Symlink servers directory for easier mounts.
RUN ln -s /opt/ol/wlp/usr/servers /servers
ADD --chown=1001:0 target/liberty/wlp/usr/servers /servers/
RUN chmod -R ug+rX  /servers/esdl-drive-server
ADD --chown=1001:0 target/esdl-drive.war /servers/esdl-drive-server/apps
USER 1001
# copy certificates
RUN mkdir -p /servers/esdl-drive-server/resources/security
COPY certificates/public.p12 /servers/esdl-drive-server/resources/security/
# delete dev or prod files
ARG mode
ENV mode=${mode:-prod}
RUN if [ "x$mode" = "xprod" ] ; then rm /servers/esdl-drive-server/dev*.xml; else echo "Building development image"; fi
# Run the server script and start the defaultServer by default.
ENTRYPOINT ["/opt/ol/wlp/bin/server", "run"]
CMD ["esdl-drive-server"]
