# Camel-Quarkus-RHOAM-Webhook-Handler-Api project

This project leverages **Red Hat build of Quarkus 1.7.x**, the Supersonic Subatomic Java Framework.

It exposes the following RESTful service endpoints  using **Apache Camel REST DSL** and the **Apache Camel Quarkus Platform HTTP** extension:
- `/webhook/amqpbridge` : Sends RHOAM Admin/Developer Portal webhook XML event to an AMQP queue through the `POST` HTTP method.
- `/openapi.json`: returns the OpenAPI 3.0 specification for the service.
- `/health` : returns the _Camel Quarkus MicroProfile_ health checks
- `/metrics` : the _Camel Quarkus MicroProfile_ metrics

## Prerequisites
- JDK 11 installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.6.2+

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```
./mvnw quarkus:dev
```