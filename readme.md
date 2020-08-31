# ESDL Drive API

## Introduction
This project provides a REST interface for Eclipse CDO (Connected Data Objects) and can be integrated in the MapEditor.

It uses Open Liberty as Java server runtime. It supports Eclipse Microprofile and openIdConnect out of the box.


## Building

To generate a jar that can be run in an Open Liberty server run:

`mvn liberty:package`

To build the docker container run:

`docker build . -t esdl-drive`


To push it to the CI repo run:

`docker tag esdl-drive:latest ci.tno.nl:5432/esdl/esdl-drive:latest`


## Environmental variables
The software can be configured using `microprofile.properties`, `server.env` and `server.xml` files, or using Environmental variables using the following properties:

#### CDO server host and port:
`CDO_SERVER_HOST=localhost:2036`
#### Repository name at the CDO server
`CDO_REPOSITORY_NAME=esdldrive`










# Liberty information:

## Introduction


The generation of the executable jar file can be performed by issuing the following command

    mvn clean package

This will create an executable jar file **hub.jar** within the _target_ maven folder. This can be started by executing the following command

    java -jar target/hub.jar


### Liberty Dev Mode

During development, you can use Liberty's development mode (dev mode) to code while observing and testing your changes on the fly.
With the dev mode, you can code along and watch the change reflected in the running server right away; 
unit and integration tests are run on pressing Enter in the command terminal; you can attach a debugger to the running server at any time to step through your code.

    mvn liberty:dev



To launch the test page, open your browser at the following URL

    http://localhost:9080/index.html


### Metrics

The Metrics exports _Telemetric_ data in a uniform way of system and custom resources. Specification [here](https://microprofile.io/project/eclipse/microprofile-metrics)

See [Metrics endpoint](/metrics) for the metrics data. You need to have an admin role to view this data.


### Open API

Exposes the information about your endpoints in the format of the OpenAPI v3 specification. Specification [here](https://microprofile.io/project/eclipse/microprofile-open-api)

See [OpenAPI UI](/openapi/ui) to access the user interface.


### Open Tracing

Allow the participation in distributed tracing of your requests through various micro services. Specification [here](https://microprofile.io/project/eclipse/microprofile-opentracing)





