# Introduction

This project provides processing, messaging and validation extensions in support of the LDES test cases.

The service is implemented in Java, using the [Spring Boot framework](https://spring.io/projects/spring-boot). It is 
built and packaged using [Apache Maven](https://maven.apache.org/), and also via Docker Compose.

In terms of implementation, all GITB-specific code is placed in package `be.vlaanderen.ldes.gitb`. This keeps the 
implementations of operations decoupled from Test Bed libraries and can facilitate unit testing.

# Prerequisites

The following prerequisites are required:
* To build: JDK 17+, Maven 3.2+.
* To run: JRE 17+.

# Building and running

1. Build using `mvn clean package`.
2. Once built you can run the application in two ways:  
  a. With maven: `mvn spring-boot:run`.  
  b. Standalone: `java -jar ./target/test-services.jar`.
3. The service endpoints are accessible at:
  a. Validation service: http://localhost:8181/ldes/services/validation?wsdl.
  b. Processing service: http://localhost:8181/ldes/services/process?wsdl.
  c. Messaging service: http://localhost:8181/ldes/services/messaging?wsdl.

## Live reload for development

This project uses Spring Boot's live reloading capabilities. When running the application from your IDE or through
Maven, any change in classpath resources is automatically detected to restart the application.

# Using Docker

To build and package this application you can also use Docker Compose. Run `docker compose build` to build the application's
image and `docker compose up -d` to run it.