# Configuring OpenIDConnect client in Liberty for keycloak

## Import the certificate
The keycloak SSL certificate must be added to the keystore.
This keystore is generated at startup in `target/liberty/wlp/usr/servers/hub/resources/security`. At startup it also generates a self-signed certificate for localhost. 

The filename of the keystore is specified in `server.xml`:

`<keyStore id="defaultKeyStore" location="public.p12" type="PKCS12" password="*****" />`

To add the certificate to the keystore do:

`$ keytool -importcert -alias letsencrypt -trustcacerts -file /mnt/c/Users/werkmane/Downloads/idm-hesi-energy-chain.pem -keystore public.p12`

And add the esdl-mapeditor realm public key (see keycloak admin page to get it) for JWT check
`$ keytool -importcert -alias esdl-mapeditor -file /mnt/c/Users/werkmane/Downloads/esdl-mapeditor-public_key.pem -trustcacerts -keystore public.p12`


# MicroProfile generated Application

## Introduction

MicroProfile Starter has generated this MicroProfile application for you.

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

## Specification examples

By default, there is always the creation of a JAX-RS application class to define the path on which the JAX-RS endpoints are available.

Also, a simple Hello world endpoint is created, have a look at the class **HelloController**.

More information on MicroProfile can be found [here](https://microprofile.io/)












### Open API

Exposes the information about your endpoints in the format of the OpenAPI v3 specification. Specification [here](https://microprofile.io/project/eclipse/microprofile-open-api)

The index page contains a link to the OpenAPI information of your endpoints.





