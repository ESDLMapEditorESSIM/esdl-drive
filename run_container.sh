docker run -p 9080:9080 -p 9443:9443 --network test --rm  --add-host=host.docker.internal:host-gateway -e CDO_SERVER_HOST=cdo-server:2036 -e POSTGRES_HOST=host.docker.internal esdl-drive:25.08 
#docker run -p 9080:9080 -p 9443:9443 --rm -e CDO_SERVER_HOST=host.docker.internal:2036 esdl-drive:24.02
