# package ESDl Drive application
mvn clean package
# create Liberty Web server
mvn liberty:create
docker build . -t esdl-drive:21.06 --build-arg mode=dev
