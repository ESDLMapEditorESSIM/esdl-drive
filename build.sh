mvn package
docker build . -t esdl-drive:21.06 --build-arg mode=dev
