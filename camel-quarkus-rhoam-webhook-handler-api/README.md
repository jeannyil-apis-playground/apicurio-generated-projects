# Camel-Quarkus-RHOAM-Webhook-Handler-Api project

This project leverages **Red Hat build of Quarkus 2.7.x**, the Supersonic Subatomic Java Framework. More specifically, the project is implemented using [**Red Hat Camel Extensions for Quarkus 2.7.x**](https://access.redhat.com/documentation/en-us/red_hat_integration/2022.q3/html/getting_started_with_camel_extensions_for_quarkus/index).

It exposes the following RESTful service endpoints  using **Apache Camel REST DSL** and the **Apache Camel Quarkus Platform HTTP** extension:
- `/webhook/amqpbridge` : 
    - Webhook ping endpoint through the `GET` HTTP method.
    - Sends RHOAM Admin/Developer Portal webhook XML event to an AMQP address (`RHOAM.WEBHOOK.EVENTS.QUEUE`) through the `POST` HTTP method.
- `/openapi.json`: returns the OpenAPI 3.0 specification for the service.
- `/q/health` : returns the _Camel Quarkus MicroProfile_ health checks
- `/q/metrics` : the _Camel Quarkus MicroProfile_ metrics

Moreover, this project leverages the [**Quarkus Kubernetes-Config** extenstion](https://quarkus.io/guides/kubernetes-config) in order to customize the run-time AMQP broker connection parameters according to your environment through the `quarkus-amqpbroker-connection-secret` secret. For instance:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: quarkus-amqpbroker-connection-secret
stringData:
  integrations-broker.url: amqps://amq-ssl-broker-amqp-0-svc.amq7-broker-cluster.svc:5672?transport.trustAll=true&transport.verifyHost=false&amqp.idleTimeout=120000
  integrations-broker.username: camel-quarkus-rhoam-webhook-handler-api
  integrations-broker.password: P@ssw0rd
  integrations-broker.pool-max-connections: "1"
  integrations-broker.max-sessions-per-connection: "500"
type: Opaque
```

## Prerequisites
- JDK 17 installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.8.1+
- An [**AMQP 1.0 protocol**](https://www.amqp.org/) compliant broker should already be installed and running. [**Red Hat AMQ 7.10 broker on OpenShift**](https://access.redhat.com/documentation/en-us/red_hat_amq_broker/7.10/html/deploying_amq_broker_on_openshift/index) with an SSL-enabled AMQP acceptor has been used for testing.
- **OPTIONAL**: [**Jaeger**](https://www.jaegertracing.io/), a distributed tracing system for observability ([_open tracing_](https://opentracing.io/)). :bulb: A simple way of starting a Jaeger tracing server is with `docker` or `podman`:
    1. Start the Jaeger tracing server:
        ```
        podman run --rm -e COLLECTOR_ZIPKIN_HOST_PORT=:9411 -e COLLECTOR_OTLP_ENABLED=true \
        -p 6831:6831/udp -p 6832:6832/udp \
        -p 5778:5778 -p 16686:16686 -p 14268:14268 -p 9411:9411 \
        quay.io/jaegertracing/all-in-one:latest
        ```
    2. While the server is running, browse to http://localhost:16686 to view tracing events.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application locally

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -Dquarkus.kubernetes-config.enabled=false -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -Dquarkus.kubernetes-config.enabled=false -jar target/*-runner.jar`.


According to your environment, you may want to customize:
- The **AMQP broker connection parameters** by adding the following run-time _system properties_:
    - `integrations-broker.url`
    - `integrations-broker.username`
    - `integrations-broker.password`
- The Jaeger collector endpoint by adding the following run-time _system properties_:
    - `quarkus.jaeger.endpoint`

Example:
```
java -Dintegrations-broker.url="amqps://amq-ssl-broker-amqp-0-svc-rte-amq7-broker-cluster.apps.jeannyil.sandbox1789.opentlc.com:443?transport.trustAll=true&transport.verifyHost=false&amqp.idleTimeout=120000" -Dquarkus.jaeger.endpoint="http://localhost:14268/api/traces" -jar target/quarkus-app/quarkus-run.jar
```

## :bulb: Packaging and running the application on Red Hat OpenShift

### Pre-requisites
- Access to a [Red Hat OpenShift](https://access.redhat.com/documentation/en-us/openshift_container_platform) cluster v3 or v4
- User has self-provisioner privilege or has access to a working OpenShift project
- An [**AMQP 1.0 protocol**](https://www.amqp.org/) compliant broker should already be installed and running. [**Red Hat AMQ 7.10 broker on OpenShift**](https://access.redhat.com/documentation/en-us/red_hat_amq_broker/7.10/html/deploying_amq_broker_on_openshift/index) with an SSL-enabled AMQP acceptor has been used for testing.

1. Login to the OpenShift cluster
    ```shell script
    oc login ...
    ```

2. Create an OpenShift project or use your existing OpenShift project. For instance, to create `ceq-services-jvm`
    ```shell script
    oc new-project ceq-services-jvm --display-name="Red Hat Camel Extensions for Quarkus Apps - JVM Mode"
    ```
        
3. Create an `allInOne` Jaeger instance.
    1. **IF NOT ALREADY INSTALLED**:
        1. Install, via OLM, the `Red Hat OpenShift distributed tracing platform` (Jaeger) operator with an `AllNamespaces` scope. :warning: Needs `cluster-admin` privileges
            ```shell script
            oc apply -f - <<EOF
            apiVersion: operators.coreos.com/v1alpha1
            kind: Subscription
            metadata:
                name: jaeger-product
                namespace: openshift-operators
            spec:
                channel: stable
                installPlanApproval: Automatic
                name: jaeger-product
                source: redhat-operators
                sourceNamespace: openshift-marketplace
            EOF
            ```
        2. Verify the successful installation of the `Red Hat OpenShift distributed tracing platform` operator
            ```shell script
            watch oc get sub,csv
            ```
    2. Create the `allInOne` Jaeger instance.
        ```shell script
        oc apply -f - <<EOF
        apiVersion: jaegertracing.io/v1
        kind: Jaeger
        metadata:
            name: jaeger-all-in-one-inmemory
        spec:
            allInOne:
                options:
                log-level: info
            strategy: allInOne
        EOF
        ```

4. Use either the _**S2I binary workflow**_ or _**S2I source workflow**_ to deploy the `camel-quarkus-rhoam-webhook-handler-api` app as described below.

### OpenShift S2I binary workflow 

This leverages the **Quarkus OpenShift** extension and is only recommended for development and testing purposes.

```shell script
./mvnw clean package -Dquarkus.kubernetes.deploy=true -Dquarkus.container-image.group=ceq-services-jvm
```

### OpenShift S2I source workflow (recommended for PRODUCTION use)

1. Make sure the latest supported OpenJDK 11 image is imported in OpenShift
    ```shell script
    oc import-image --confirm openjdk-17-ubi8 \
    --from=registry.access.redhat.com/ubi8/openjdk-17:1.11 \
    -n openshift
    ```

2. Create the `view-secrets` role and bind it, along with the `view` cluster role, to the `default` service account used to run the quarkus application. These permissions allow the `default` service account to access secrets.
    ```shell script
    oc apply -f <(echo '
    ---
    apiVersion: rbac.authorization.k8s.io/v1
    kind: Role
    metadata:
      name: view-secrets
    rules:
      - apiGroups:
          - ""
        resources:
          - secrets
        verbs:
          - get
    ---
    apiVersion: rbac.authorization.k8s.io/v1
    kind: RoleBinding
    metadata:
      name: default:view
    roleRef:
      kind: ClusterRole
      apiGroup: rbac.authorization.k8s.io
      name: view
    subjects:
      - kind: ServiceAccount
        name: default
    ---
    apiVersion: rbac.authorization.k8s.io/v1
    kind: RoleBinding
    metadata:
      name: default:view-secrets
    roleRef:
      kind: Role
      apiGroup: rbac.authorization.k8s.io
      name: view-secrets
    subjects:
      - kind: ServiceAccount
        name: default
    ')
    ```

3. Create the `camel-quarkus-rhoam-webhook-handler-api` OpenShift application from the git repository
    ```shell script
    oc new-app https://github.com/jeannyil-apis-playground/apicurio-generated-projects.git \
    --context-dir=camel-quarkus-rhoam-webhook-handler-api \
    --name=camel-quarkus-rhoam-webhook-handler-api \
    --image-stream="openshift/openjdk-17-ubi8" \
    --labels=app.openshift.io/runtime=quarkus
    ```

4. Follow the log of the S2I build
    ```shell script
    oc logs bc/camel-quarkus-rhoam-webhook-handler-api -f
    ```
    ```shell script
    Cloning "https://github.com/jeannyil-apis-playground/apicurio-generated-projects.git" ...
            Commit: bcb6e69e2f0285ef1e9dcdb4abb47ede80fb43e1 (Adapted S2I Configuration to RH build of Quarkus v2.2.3.Final-redhat-00013)
            Author: jeanNyil <jean.nyilimbibi@gmail.com>
            Date:   Wed Nov 24 13:32:03 2021 +0100
    [...]
    Successfully pushed image-registry.openshift-image-registry.svc:5000/ceq-services-jvm/camel-quarkus-rhoam-webhook-handler-api@sha256:9932efcb67f775fcecef2055892c01ac337f64d7cc55f96197edda537536f424
    Push successful
    ```

5. Create a non-secure route to expose the `camel-quarkus-rhoam-webhook-handler-api` service outside the OpenShift cluster
    ```shell script
    oc expose svc/camel-quarkus-rhoam-webhook-handler-api
    ```

## :bulb: Testing the application on OpenShift

### Pre-requisites

- [**`curl`**](https://curl.se/) or [**`HTTPie`**](https://httpie.io/) command line tools. 
- [**`HTTPie`**](https://httpie.io/) has been used in the tests.

### Testing instructions:

1. Get the OpenShift route hostname
    ```shell script
    URL="http://$(oc get route camel-quarkus-rhoam-webhook-handler-api -o jsonpath='{.spec.host}')"
    ```
    
2. Test the `/webhook/amqpbridge` endpoint

    - `GET /webhook/amqpbridge` :

        ```shell script
        http -v $URL/webhook/amqpbridge
        ```
        ```shell script
        [...]
        HTTP/1.1 200 OK
        [...]
        Content-Type: application/json
        [...]
        breadcrumbId: 43EB8F0221CD24E-0000000000000001
        transfer-encoding: chunked

        {
            "status": "OK"
        }
        ```

    - `POST /webhook/amqpbridge` :

        - `OK` response:

            ```shell script
            echo '<?xml version="1.0" encoding="UTF-8"?>
            <event>
            <action>updated</action>
            <type>account</type>
            <object>
                <account>
                <id>6</id>
                <created_at>2021-05-14T20:22:53Z</created_at>
                <updated_at>2021-05-14T20:22:53Z</updated_at>
                <state>approved</state>
                <org_name>TestAccount</org_name>
                <extra_fields/>
                <monthly_billing_enabled>true</monthly_billing_enabled>
                <monthly_charging_enabled>true</monthly_charging_enabled>
                <credit_card_stored>false</credit_card_stored>
                <plans>
                    <plan default="true">
                    <id>6</id>
                    <name>Default</name>
                    <type>account_plan</type>
                    <state>hidden</state>
                    <approval_required>false</approval_required>
                    <setup_fee>0.0</setup_fee>
                    <cost_per_month>0.0</cost_per_month>
                    <trial_period_days/>
                    <cancellation_period>0</cancellation_period>
                    </plan>
                </plans>
                <users>
                    <user>
                    <id>9</id>
                    <created_at>2021-05-14T20:22:53Z</created_at>
                    <updated_at>2021-05-14T20:22:53Z</updated_at>
                    <account_id>6</account_id>
                    <state>pending</state>
                    <role>admin</role>
                    <username>admin</username>
                    <email>admin@acme.org</email>
                    <extra_fields/>
                    </user>
                </users>
                </account>
            </object>
            </event>' | http -v POST $URL/webhook/amqpbridge content-type:application/xml
            ```
            ```shell script
            [...]
            HTTP/1.1 200 OK
            Access-Control-Allow-Headers: Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers
            Access-Control-Allow-Methods: GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, CONNECT, PATCH
            Access-Control-Allow-Origin: *
            Access-Control-Max-Age: 3600
            Content-Type: application/json
            RHOAM_EVENT_ACTION: updated
            RHOAM_EVENT_TYPE: account
            Set-Cookie: 0d5acfcb0ca2b6f2520831b8d4bd4031=f3580c9af577adb49be04813506f5ec6; path=/; HttpOnly
            breadcrumbId: 43EB8F0221CD24E-0000000000000002
            transfer-encoding: chunked

            {
                "status": "OK"
            }
            ```

        - `KO` response:

            ```shell script
            echo 'PLAIN TEXT' | http -v POST $URL/webhook/amqpbridge content-type:application/xml
            ```
            ```shell script
            [...]
            HTTP/1.1 400 Bad Request
            Access-Control-Allow-Headers: Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers
            Access-Control-Allow-Methods: GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, CONNECT, PATCH
            Access-Control-Allow-Origin: *
            Access-Control-Max-Age: 3600
            Content-Type: application/json
            Set-Cookie: 0d5acfcb0ca2b6f2520831b8d4bd4031=f3580c9af577adb49be04813506f5ec6; path=/; HttpOnly
            breadcrumbId: 43EB8F0221CD24E-0000000000000003
            transfer-encoding: chunked

            {
                "error": {
                    "code": "400",
                    "description": "Bad Request",
                    "message": "org.apache.camel.TypeConversionException: Error during type conversion from type: java.lang.String to the required type: org.w3c.dom.Document with value PLAIN TEXT\n due to org.xml.sax.SAXParseException: Content is not allowed in prolog."
                },
                "status": "KO"
            }
            ```

3. Test the `/openapi.json` endpoint
    ```shell script
    http -v $URL/openapi.json
    ```
    ```shell script
    [...]
    HTTP/1.1 200 OK
    Accept: */*
    [...]
    Content-Type: application/vnd.oai.openapi+json
    [...]
    breadcrumbId: DFB5B53061B9578-0000000000000002
    transfer-encoding: chunked

    {
        "components": {
            "schemas": {
                "ErrorMessageType": {
                    "description": "Error message type  ",
    [...]
        "info": {
        "contact": {
            "name": "Jean Nyilimbibi"
        },
        "description": "API that handles RHOAM Admin/Developer Portals webhook events",
        "license": {
            "name": "MIT License",
            "url": "https://opensource.org/licenses/MIT"
        },
        "title": "RHOAM Webhook Events Handler API",
        "version": "1.0.0"
    },
    "openapi": "3.0.2",
    [...]
    },
        "servers": [
            {
                "description": "API Backend URL",
                "url": "http://rhoam-webhook-events-handler-api.apps.jeannyil.sandbox1789.opentlc.com"
            }
        ]
    }
    ```

4. Test the `/q/health` endpoint
    ```shell script
    http -v $URL/q/health
    ```
    ```shell script
    HTTP/1.1 200 OK
    Set-Cookie: 0d5acfcb0ca2b6f2520831b8d4bd4031=2cd3ddf89a39206056fcce59b93f59aa; path=/; HttpOnly
    cache-control: private
    content-length: 647
    content-type: application/json; charset=UTF-8
    set-cookie: 0d5acfcb0ca2b6f2520831b8d4bd4031=2cd3ddf89a39206056fcce59b93f59aa; path=/; HttpOnly

    {
        "checks": [
            {
                "name": "camel-liveness-checks",
                "status": "UP"
            },
            {
                "data": {
                    "context": "UP",
                    "route:generate-error-response-route": "UP",
                    "route:get-openapi-spec-route": "UP",
                    "route:ping-webhook-route": "UP",
                    "route:send-to-amqp-queue-route": "UP",
                    "route:webhook-amqpbridge-handler-route": "UP",
                    "route:webhook-amqpbridge-ping-route": "UP"
                },
                "name": "camel-readiness-checks",
                "status": "UP"
            }
        ],
        "status": "UP"
    }
    ```

5. Test the `/q/health/live` endpoint
    ```shell script
    http -v $URL/q/health/live
    ```
    ```shell script
    HTTP/1.1 200 OK
    Set-Cookie: 0d5acfcb0ca2b6f2520831b8d4bd4031=a9c4acc377212f7232380c78ddbbf21f; path=/; HttpOnly
    cache-control: private
    content-length: 138
    content-type: application/json; charset=UTF-8
    set-cookie: 0d5acfcb0ca2b6f2520831b8d4bd4031=a9c4acc377212f7232380c78ddbbf21f; path=/; HttpOnly

    {
        "checks": [
            {
                "name": "camel-liveness-checks",
                "status": "UP"
            }
        ],
        "status": "UP"
    }
    ```

6. Test the `/q/health/ready` endpoint
    ```shell script
    http -v $URL/q/health/ready
    ```
    ```shell script
    HTTP/1.1 200 OK
    Set-Cookie: 0d5acfcb0ca2b6f2520831b8d4bd4031=a9c4acc377212f7232380c78ddbbf21f; path=/; HttpOnly
    cache-control: private
    content-length: 554
    content-type: application/json; charset=UTF-8
    set-cookie: 0d5acfcb0ca2b6f2520831b8d4bd4031=a9c4acc377212f7232380c78ddbbf21f; path=/; HttpOnly

    {
        "checks": [
            {
                "data": {
                    "context": "UP",
                    "route:generate-error-response-route": "UP",
                    "route:get-openapi-spec-route": "UP",
                    "route:ping-webhook-route": "UP",
                    "route:send-to-amqp-queue-route": "UP",
                    "route:webhook-amqpbridge-handler-route": "UP",
                    "route:webhook-amqpbridge-ping-route": "UP"
                },
                "name": "camel-readiness-checks",
                "status": "UP"
            }
        ],
        "status": "UP"
    }
    ```

7. Test the `/q/metrics` endpoint
    ```shell script
    http -v $URL/q/metrics
    ```
    ```shell script
    [...]
    HTTP/1.1 200 OK
    Access-Control-Allow-Credentials: true
    Access-Control-Allow-Headers: origin, content-type, accept, authorization
    Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, HEAD
    Access-Control-Allow-Origin: *
    Access-Control-Max-Age: 1209600
    Cache-control: private
    Content-Type: text/plain
    Set-Cookie: 0d5acfcb0ca2b6f2520831b8d4bd4031=c1552cc3572cec4462da37f30dd5423d; path=/; HttpOnly
    content-length: 38276
    [...]
    # HELP application_camel_context_exchanges_total The total number of exchanges for a route or Camel Context
    # TYPE application_camel_context_exchanges_total counter
    application_camel_context_exchanges_total{camelContext="camel-1"} 5.0
    [...]
    # HELP application_camel_route_count The count of routes.
    # TYPE application_camel_route_count gauge
    application_camel_route_count{camelContext="camel-1"} 6.0
    # HELP application_camel_route_exchanges_completed_total The total number of completed exchanges for a route or Camel Context
    # TYPE application_camel_route_exchanges_completed_total counter
    application_camel_route_exchanges_completed_total{camelContext="camel-1",routeId="generate-error-response-route"} 0.0
    application_camel_route_exchanges_completed_total{camelContext="camel-1",routeId="get-openapi-spec-route"} 1.0
    application_camel_route_exchanges_completed_total{camelContext="camel-1",routeId="ping-webhook-route"} 0.0
    application_camel_route_exchanges_completed_total{camelContext="camel-1",routeId="send-to-amqp-queue-route"} 1.0
    application_camel_route_exchanges_completed_total{camelContext="camel-1",routeId="webhook-amqpbridge-handler-route"} 1.0
    application_camel_route_exchanges_completed_total{camelContext="camel-1",routeId="webhook-amqpbridge-ping-route"} 0.0
    [...]
    # HELP application_camel_route_exchanges_total The total number of exchanges for a route or Camel Context
    # TYPE application_camel_route_exchanges_total counter
    application_camel_route_exchanges_total{camelContext="camel-1",routeId="generate-error-response-route"} 2.0
    application_camel_route_exchanges_total{camelContext="camel-1",routeId="get-openapi-spec-route"} 1.0
    application_camel_route_exchanges_total{camelContext="camel-1",routeId="ping-webhook-route"} 1.0
    application_camel_route_exchanges_total{camelContext="camel-1",routeId="send-to-amqp-queue-route"} 3.0
    application_camel_route_exchanges_total{camelContext="camel-1",routeId="webhook-amqpbridge-handler-route"} 3.0
    application_camel_route_exchanges_total{camelContext="camel-1",routeId="webhook-amqpbridge-ping-route"} 1.0
    [...]
    ```

## Creating a native executable

### :bulb: Running locally

You can create a native executable using: `./mvnw package -Pnative`.

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: `./mvnw package -Pnative -Dquarkus.native.container-build=true`.

You can then execute your native executable with: `./target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/building-native-image.

### :bulb: Deploying the native executable as an _OpenShift Serverless_ service

#### Prerequisites

- OpenShift Serverless operator is installed. See [Installing the OpenShift Serverless Operator](https://access.redhat.com/documentation/en-us/openshift_container_platform/4.11/html/serverless/install#serverless-install-web-console_install-serverless-operator).
- OpenShift Knative Serving is installed and verified. See [Installing Knative Serving](https://access.redhat.com/documentation/en-us/openshift_container_platform/4.11/html/serverless/install#installing-knative-serving).
- For native compilation, a Linux X86_64 operating system or an OCI (Open Container Initiative) compatible container runtime, such as _[Podman](https://podman.io/)_ or _[Docker](https://www.docker.com/)_ is required.
- User has self-provisioner privilege or has access to a working OpenShift project
- An [**AMQP 1.0 protocol**](https://www.amqp.org/) compliant broker should already be installed and running. [**Red Hat AMQ 7.10 broker on OpenShift**](https://access.redhat.com/documentation/en-us/red_hat_amq_broker/7.10/html/deploying_amq_broker_on_openshift/index) with an SSL-enabled AMQP acceptor has been used for testing.

#### Instructions

1. Login to the OpenShift cluster
    ```shell script
    oc login ...
    ```

2. Create an OpenShift project or use your existing OpenShift project. For instance, to create `ceq-services-serverless`
    ```shell script
    oc new-project ceq-services-serverless --display-name="Red Hat Camel Extensions for Quarkus Apps - Native Mode and Serverless"
    ```
        
3. Create an `allInOne` Jaeger instance.
    1. **IF NOT ALREADY INSTALLED**:
        1. Install, via OLM, the `Red Hat OpenShift distributed tracing platform` (Jaeger) operator with an `AllNamespaces` scope. :warning: Needs `cluster-admin` privileges
            ```shell script
            oc apply -f - <<EOF
            apiVersion: operators.coreos.com/v1alpha1
            kind: Subscription
            metadata:
                name: jaeger-product
                namespace: openshift-operators
            spec:
                channel: stable
                installPlanApproval: Automatic
                name: jaeger-product
                source: redhat-operators
                sourceNamespace: openshift-marketplace
            EOF
            ```
        2. Verify the successful installation of the `Red Hat OpenShift distributed tracing platform` operator
            ```shell script
            watch oc get sub,csv
            ```
    2. Create the `allInOne` Jaeger instance.
        ```shell script
        oc apply -f - <<EOF
        apiVersion: jaegertracing.io/v1
        kind: Jaeger
        metadata:
            name: jaeger-all-in-one-inmemory
        spec:
            allInOne:
                options:
                log-level: info
            strategy: allInOne
        EOF
        ```

4. Package and deploy to OpenShift
    -  Using podman to build the native binary:
        ```shell script
        ./mvnw clean package -Pnative -Dquarkus.native.container-runtime=podman \
        -Dquarkus.native.builder-image=registry.access.redhat.com/quarkus/mandrel-21-jdk17-rhel8:latest \
        -Dquarkus.kubernetes.deploy=true \
        -Dquarkus.kubernetes.deployment-target=knative \
        -Dquarkus.container-image.group=ceq-services-serverless
        ```
    -  Using docker to build the native binary:
        ```shell script
	    ./mvnw clean package -Pnative -Dquarkus.native.container-runtime=docker \
        -Dquarkus.native.builder-image=registry.access.redhat.com/quarkus/mandrel-21-jdk17-rhel8:latest \
        -Dquarkus.kubernetes.deploy=true \
        -Dquarkus.kubernetes.deployment-target=knative \
        -Dquarkus.container-image.group=ceq-services-serverless
        ```

## :bulb: Start-up time comparison on the same OpenShift cluster

- OpenShift Container Platform 4.11.16 running on AWS
- Compute nodes types: [m5a.4xlarge](https://aws.amazon.com/ec2/instance-types/m5/) (16 vCPU / 64 GiB Memory)

### JVM mode - **2.594s**

```shell script
2022-11-27 20:50:48,033 INFO  [org.apa.cam.qua.cor.CamelBootstrapRecorder] (main) Bootstrap runtime: org.apache.camel.quarkus.main.CamelMainRuntime
2022-11-27 20:50:48,076 INFO  [org.apa.cam.mai.BaseMainSupport] (main) Auto-configuration summary
2022-11-27 20:50:48,077 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     camel.context.name=camel-quarkus-rhoam-webhook-handler-api
2022-11-27 20:50:48,278 INFO  [org.apa.cam.lan.xpa.XPathBuilder] (main) Created default XPathFactory com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl@6d6bff89
2022-11-27 20:50:48,301 INFO  [org.mes.poo.jms.JmsPoolConnectionFactory] (main) Provided ConnectionFactory implementation is JMS 2.0+ capable.
2022-11-27 20:50:48,420 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) StreamCaching is enabled on CamelContext: camel-quarkus-rhoam-webhook-handler-api
2022-11-27 20:50:48,433 INFO  [org.apa.cam.imp.eng.DefaultStreamCachingStrategy] (main) StreamCaching in use with spool directory: /tmp/camel/camel-tmp-7D1C7036D3CC53E-0000000000000000 and rules: [Spool > 128K body size]
2022-11-27 20:50:48,444 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Routes startup (total:6 started:6)
2022-11-27 20:50:48,444 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started generate-error-response-route (direct://generateErrorResponse)
2022-11-27 20:50:48,444 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started send-to-amqp-queue-route (direct://sendToAMQPQueue)
2022-11-27 20:50:48,444 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started ping-webhook-route (direct://pingWebhook)
2022-11-27 20:50:48,444 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started get-openapi-spec-route (rest://get:/openapi.json)
2022-11-27 20:50:48,445 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started webhook-amqpbridge-ping-route (rest://get:/webhook/amqpbridge)
2022-11-27 20:50:48,445 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started webhook-amqpbridge-handler-route (rest://post:/webhook/amqpbridge)
2022-11-27 20:50:48,445 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.14.2.redhat-00047 (camel-quarkus-rhoam-webhook-handler-api) started in 292ms (build:0ms init:267ms start:25ms)
2022-11-27 20:50:48,541 INFO  [io.quarkus] (main) camel-quarkus-rhoam-webhook-handler-api 1.0.0 on JVM (powered by Quarkus 2.7.6.Final-redhat-00006) started in 2.594s. Listening on: http://0.0.0.0:8080
2022-11-27 20:50:48,541 INFO  [io.quarkus] (main) Profile prod activated.
2022-11-27 20:50:48,541 INFO  [io.quarkus] (main) Installed features: [camel-attachments, camel-bean, camel-core, camel-direct, camel-jackson, camel-jms, camel-microprofile-health, camel-microprofile-metrics, camel-openapi-java, camel-opentracing, camel-platform-http, camel-rest, camel-xpath, cdi, config-yaml, jaeger, kubernetes, kubernetes-client, qpid-jms, smallrye-context-propagation, smallrye-health, smallrye-metrics, smallrye-opentracing, vertx]
```

### Native mode - **0.162s**

```shell script
2022-11-27 21:44:27,061 INFO  [org.apa.cam.qua.cor.CamelBootstrapRecorder] (main) Bootstrap runtime: org.apache.camel.quarkus.main.CamelMainRuntime
2022-11-27 21:44:27,064 INFO  [org.apa.cam.mai.BaseMainSupport] (main) Auto-configuration summary
2022-11-27 21:44:27,064 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     camel.context.name=camel-quarkus-rhoam-webhook-handler-api
2022-11-27 21:44:27,067 INFO  [org.apa.cam.lan.xpa.XPathBuilder] (main) Created default XPathFactory com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl@6e1cf917
2022-11-27 21:44:27,068 INFO  [org.mes.poo.jms.JmsPoolConnectionFactory] (main) Provided ConnectionFactory implementation is JMS 2.0+ capable.
2022-11-27 21:44:27,074 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) StreamCaching is enabled on CamelContext: camel-quarkus-rhoam-webhook-handler-api
2022-11-27 21:44:27,075 INFO  [org.apa.cam.imp.eng.DefaultStreamCachingStrategy] (main) StreamCaching in use with spool directory: /tmp/camel/camel-tmp-DBA2E4F75088EE3-0000000000000000 and rules: [Spool > 128K body size]
2022-11-27 21:44:27,076 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Routes startup (total:6 started:6)
2022-11-27 21:44:27,076 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started send-to-amqp-queue-route (direct://sendToAMQPQueue)
2022-11-27 21:44:27,076 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started ping-webhook-route (direct://pingWebhook)
2022-11-27 21:44:27,076 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started generate-error-response-route (direct://generateErrorResponse)
2022-11-27 21:44:27,076 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started get-openapi-spec-route (rest://get:/openapi.json)
2022-11-27 21:44:27,076 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started webhook-amqpbridge-ping-route (rest://get:/webhook/amqpbridge)
2022-11-27 21:44:27,076 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started webhook-amqpbridge-handler-route (rest://post:/webhook/amqpbridge)
2022-11-27 21:44:27,076 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.14.2.redhat-00047 (camel-quarkus-rhoam-webhook-handler-api) started in 11ms (build:0ms init:9ms start:2ms)
2022-11-27 21:44:27,078 INFO  [io.quarkus] (main) camel-quarkus-rhoam-webhook-handler-api 1.0.0 native (powered by Quarkus 2.7.6.Final-redhat-00006) started in 0.162s. Listening on: http://0.0.0.0:8080
2022-11-27 21:44:27,078 INFO  [io.quarkus] (main) Profile prod activated.
2022-11-27 21:44:27,078 INFO  [io.quarkus] (main) Installed features: [camel-attachments, camel-bean, camel-core, camel-direct, camel-jackson, camel-jms, camel-microprofile-health, camel-microprofile-metrics, camel-openapi-java, camel-opentracing, camel-platform-http, camel-rest, camel-xpath, cdi, config-yaml, jaeger, kubernetes, kubernetes-client, qpid-jms, smallrye-context-propagation, smallrye-health, smallrye-metrics, smallrye-opentracing, vertx]
```

## Related Guides

- AMQP 1.0 JMS client - Apache Qpid JMS ([guide](https://quarkus.io/guides/jms)): Use JMS APIs with AMQP 1.0 servers such as ActiveMQ Artemis, ActiveMQ 5, Qpid Broker-J, Qpid Dispatch router, Azure Service Bus, and more
- OpenShift ([guide](https://quarkus.io/guides/deploying-to-openshift)): Generate OpenShift resources from annotations
- Camel Platform HTTP ([guide](https://access.redhat.com/documentation/en-us/red_hat_integration/2.latest/html/camel_extensions_for_quarkus_reference/extensions-platform-http)): Expose HTTP endpoints using the HTTP server available in the current platform
- Camel Direct ([guide](https://access.redhat.com/documentation/en-us/red_hat_integration/2.latest/html/camel_extensions_for_quarkus_reference/extensions-direct)): Call another endpoint from the same Camel Context synchronously
- Camel Jackson ([guide](https://access.redhat.com/documentation/en-us/red_hat_integration/2.latest/html/camel_extensions_for_quarkus_reference/extensions-jackson)): Marshal POJOs to JSON and back using Jackson
- YAML Configuration ([guide](https://quarkus.io/guides/config#yaml)): Use YAML to configure your Quarkus application
- Camel Bean ([guide](https://access.redhat.com/documentation/en-us/red_hat_integration/2.latest/html/camel_extensions_for_quarkus_reference/extensions-bean)): Invoke methods of Java beans
- Camel OpenTracing ([guide](https://camel.apache.org/camel-quarkus/latest/reference/extensions/opentracing.html)): Distributed tracing using OpenTracing
- Camel XPath ([guide](https://access.redhat.com/documentation/en-us/red_hat_integration/2.latest/html/camel_extensions_for_quarkus_reference/extensions-xpath)): Evaluates an XPath expression against an XML payload
- Camel OpenAPI Java ([guide](https://access.redhat.com/documentation/en-us/red_hat_integration/2.latest/html/camel_extensions_for_quarkus_reference/extensions-openapi-java)): Expose OpenAPI resources defined in Camel REST DSL
- Camel MicroProfile Health ([guide](https://access.redhat.com/documentation/en-us/red_hat_integration/2.latest/html/camel_extensions_for_quarkus_reference/extensions-microprofile-health)): Expose Camel health checks via MicroProfile Health
- Camel MicroProfile Metrics ([guide](https://access.redhat.com/documentation/en-us/red_hat_integration/2.latest/html/camel_extensions_for_quarkus_reference/extensions-microprofile-metrics)): Expose metrics from Camel routes
- Kubernetes Config ([guide](https://quarkus.io/guides/kubernetes-config)): Read runtime configuration from Kubernetes ConfigMaps and Secrets
- Camel JMS ([guide](https://access.redhat.com/documentation/en-us/red_hat_integration/2.latest/html/camel_extensions_for_quarkus_reference/extensions-jms)): Sent and receive messages to/from a JMS Queue or Topic
- Camel Rest ([guide](https://access.redhat.com/documentation/en-us/red_hat_integration/2.latest/html/camel_extensions_for_quarkus_reference/extensions-rest)): Expose REST services and their OpenAPI Specification or call external REST services

## Provided Code

### YAML Config

Configure your application with YAML

[Related guide section...](https://quarkus.io/guides/config-reference#configuration-examples)

The Quarkus application configuration is located in `src/main/resources/application.yml`.