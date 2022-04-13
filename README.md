# ESDL Drive API

## Introduction
This project provides a REST interface for Eclipse CDO (Connected Data Objects) and can be integrated in the MapEditor.

There is also a commandline tool available to interact with the ESDLDrive and is available from [its Github repository](https://github.com/ESDLMapEditorESSIM/esdl-drive-tools).

It uses Open Liberty as Java server runtime. It supports Eclipse Microprofile and openIdConnect out of the box.

## Access control

Access to folders is based on data in the JWT token. 

Basic rule: Group membership means R+W access to that folder. Group membership is stored in the `group_path` claim and contains the full path of that folder/group, e.g. `/edr/Public` or `/Users/edwin` or `/Organizations/TNO` .

1. If the user is added to a group in Keycloak, it has automatically *write* access to that group. 
   ESDL Drive makes sure these folders exists when setting up a session to the Drive.
   
2. Groups in Keycloak can have the attribute of `esdl-drive-read-only` with the value of the  _full_  path of that group (e.g. `/Projects/ReadOnlyFolder`).
   If a user is member of this group, it's write-access is revoked. This way it is possible to create read-only folders in Keycloak (albeit a bit hacky, ACLs would be nicer), as Keycloak will aggregate all the 'esdl-drive-read-only' attributes of all groups into a nice list of folder paths.

3. A user can have additional write access to a folder (e.g. for disabling the `esdl-drive-read-only` claim) by setting the user attribute `esdl-drive-write` to a (list of) folder paths.
This gives the user direct write access to that folder (even without being a group member!) and makes it possible to give
some users write access to a folder and others not.

To add multiple folders to a users' `esdl-drive-write` attribute separate the folder paths by '##' (keycloak's way of doing this).

The idea is that option 2 does not happen a lot, and that option 3 is only for exceptional circumstances.



## Building

To generate a jar that can be run in an Open Liberty server run:

`mvn liberty:package`

To build the docker container run:

`docker build . -t esdl-drive`


To push it to the CI repo run:

`docker tag esdl-drive:latest ci.tno.nl:5432/esdl/esdl-drive:latest`


## Environmental variables
The software can be configured using `microprofile.properties`, `server.env` and `server.xml` files.
Additionally a `keycloak.xml` is used to configure keycloak. The default configuration is made for the localhost setup with a keycloak server hosted on your local machine.

Some settings can be changed by using Environmental variables using the following properties:

#### CDO server host and port:
`CDO_SERVER_HOST=localhost:2036`
#### Repository name at the CDO server
`CDO_REPOSITORY_NAME=esdldrive`

#### Disable showing EDR in browser
Defaults to:
`EDR_ENABLED=true`

Set EDR_ENABLED=0 to disable showing the EDR icon in the browser (both in MapEditor ESDL Browser as in this browser page

#### KEYCLOAK realm
`KEYCLOAK_REALM` defaults to `http://keycloak:8080/auth/realms/esdl-mapeditor`
#### KEYCLOAK external accessible URL
In localhost deployements of keycloak in docker you need to make a difference between the external accessible URL and the internal one.

`keycloak_realm_external` defaults to `{keycloak_realm}`

####  KEYCLOAK client secret for cdo-mondaine
The ESDL Drive uses `cdo-mondaine` as client name in keycloak. Its associated secret is defined using:
`keycloak_client_secret`. It defaults to the one used for the localhost deployment in docker.
Users need the group of `cdo_read` to read and write to the ESDLDrive. Normally everybody in the default keycloak setup for localhost has this role.

#### Keytool password
All keys and certificates (see certificates/certificate.md) are stored in a java keystore. You need the keytool password to access this keystore.
`keytool_password` has a default value (see also server.xml)











# Building:

The generation of the executable jar file can be performed by issuing the following command

    mvn clean package

This will create an executable jar file **esdl-drive.jar** within the _target_ maven folder. This can be started by executing the following command

    java -jar target/esdl-drive.jar


### Liberty Dev Mode

During development, you can use Liberty's development mode (dev mode) to code while observing and testing your changes on the fly.
With the dev mode, you can code along and watch the change reflected in the running server right away; 
unit and integration tests are run on pressing Enter in the command terminal; you can attach a debugger to the running server at any time to step through your code.

    mvn liberty:dev



To launch the ESDLDrive page, open your browser at the following URL

    http://localhost:9080/index.html


### Metrics

The Metrics exports _Telemetric_ data in a uniform way of system and custom resources. Specification [here](https://microprofile.io/project/eclipse/microprofile-metrics)

See [Metrics endpoint](/metrics) for the metrics data. You need to have an admin role to view this data (see keycloak.xml).


### Open API

Exposes the information about your endpoints in the format of the OpenAPI v3 specification. Specification [here](https://microprofile.io/project/eclipse/microprofile-open-api)

See [OpenAPI UI](/openapi/ui) to access the user interface.


### Open Tracing support

Allow the participation in distributed tracing of your requests through various micro services. Specification [here](https://microprofile.io/project/eclipse/microprofile-opentracing)





