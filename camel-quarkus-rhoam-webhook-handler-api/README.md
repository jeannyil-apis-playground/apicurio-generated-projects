# Camel-Quarkus-RHOAM-Webhook-Handler-Api project

This project leverages **Red Hat build of Quarkus 1.7.x**, the Supersonic Subatomic Java Framework.

It exposes the following RESTful service endpoints  using **Apache Camel REST DSL** and the **Apache Camel Quarkus Platform HTTP** extension:
- `/webhook/amqpbridge` : 
    - Webhook ping endpoint through the `GET` HTTP method.
    - Sends RHOAM Admin/Developer Portal webhook XML event to an AMQP queue through the `POST` HTTP method.
- `/openapi.json`: returns the OpenAPI 3.0 specification for the service.
- `/health` : returns the _Camel Quarkus MicroProfile_ health checks
- `/metrics` : the _Camel Quarkus MicroProfile_ metrics

## Prerequisites
- JDK 11 installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.6.2+
- The Red Hat AMQ 7 product should already be installed and running on your OpenShift installation with an SSL-enabled AMQP acceptor.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```
./mvnw quarkus:dev
```

## Packaging and running the application locally

The application can be packaged using `./mvnw package`.
It produces the `camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar` file in the `/target` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/lib` directory.

The application is now runnable using `java -jar target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar`.

## Packaging and running the application on Red Hat OpenShift

### Pre-requisites
- Access to a [Red Hat OpenShift](https://access.redhat.com/documentation/en-us/openshift_container_platform) cluster v3 or v4
- User has self-provisioner privilege or has access to a working OpenShift project

1. Login to the OpenShift cluster
    ```zsh
    oc login ...
    ```
2. Create an OpenShift project or use your existing OpenShift project. For instance, to create `camel-quarkus`
    ```zsh
    oc new-project camel-quarkus-jvm --display-name="Apache Camel Quarkus Apps - JVM Mode"
    ```
3. Use either the _**S2I binary workflow**_ or _**S2I source workflow**_ to deploy the `camel-quarkus-rhoam-webhook-handler-api` app as described below.

### OpenShift S2I binary workflow 

This leverages the _Quarkus OpenShift_ extension and is only recommended for development and testing purposes.

```zsh
./mvnw clean package -Dquarkus.kubernetes.deploy=true
```
```zsh
[...]
[INFO] [io.quarkus.deployment.pkg.steps.JarResultBuildStep] Building thin jar: /Users/jeannyil/Workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeploy] Kubernetes API Server at 'https://api.jeannyil.sandbox438.opentlc.com:6443/' successfully contacted.
[...]
[INFO] [io.quarkus.container.image.s2i.deployment.S2iProcessor] Performing s2i binary build with jar on server: https://api.jeannyil.sandbox438.opentlc.com:6443/ in namespace:camel-quarkus-jvm.
[...]
[INFO] [io.quarkus.container.image.s2i.deployment.S2iProcessor] Successfully pushed image-registry.openshift-image-registry.svc:5000/camel-quarkus-jvm/camel-quarkus-rhoam-webhook-handler-api@sha256:fc92c094ef6386ce3da9ef83164a84f6f421ed93da356524523c0ac385e1722e
[INFO] [io.quarkus.container.image.s2i.deployment.S2iProcessor] Push successful
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Deploying to openshift server: https://api.jeannyil.sandbox438.opentlc.com:6443/ in namespace: camel-quarkus-jvm.
[...]
```

### OpenShift S2I source workflow (recommended for PRODUCTION use)

1. Make sure the latest supported OpenJDK 11 image is imported in OpenShift
    ```zsh
    oc import-image --confirm openjdk-11-ubi8 \
    --from=registry.access.redhat.com/ubi8/openjdk-11 \
    -n openshift
    ```
2. Create the `camel-quarkus-rhoam-webhook-handler-api` OpenShift application from the git repository
    ```zsh
    oc new-app https://github.com/jeannyil-apis-playground/apicurio-generated-projects.git \
    --context-dir=camel-quarkus-rhoam-webhook-handler-api \
    --name=camel-quarkus-rhoam-webhook-handler-api \
    --image-stream="openshift/openjdk-11-ubi8"
    ```
3. Follow the log of the S2I build
    ```zsh
    oc logs bc/camel-quarkus-rhoam-webhook-handler-api -f
    ```
    ```zsh
    Cloning "https://github.com/jeannyil-apis-playground/apicurio-generated-projects.git" ...
        Commit: e658f9ed76fb99ff4b3c6719f9a65c01ea6d2ca0 (Minor update)
        Author: Jean Armand Nyilimbibi <jean.nyilimbibi@gmail.com>
        Date:   Sat May 15 14:27:23 2021 +0200
    [...]
    Successfully pushed image-registry.openshift-image-registry.svc:5000/camel-quarkus-jvm/camel-quarkus-rhoam-webhook-handler-api@sha256:e8e6d8e874fae9ba9616b971225b8307b6aac2911980f50d3fab2b12a51e25da
    Push successful
    ```
4. Create a non-secure route to expose the `camel-quarkus-rhoam-webhook-handler-api` service outside the OpenShift cluster
    ```zsh
    oc expose svc/camel-quarkus-rhoam-webhook-handler-api
    ```

## Testing the application on OpenShift

1. Get the OpenShift route hostname
    ```zsh
    URL="http://$(oc get route camel-quarkus-rhoam-webhook-handler-api -o jsonpath='{.spec.host}')"
    ```
2. Test the `/webhook/amqpbridge` endpoint
    - `GET /webhook/amqpbridge` :

        ```zsh
        http -v $URL/webhook/amqpbridge
        ```
        ```zsh
        GET /webhook/amqpbridge HTTP/1.1
        [...]
        User-Agent: HTTPie/2.4.0

        HTTP/1.1 200 OK
        Accept: */*
        Accept-Encoding: gzip, deflate
        Access-Control-Allow-Headers: Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers
        Access-Control-Allow-Methods: GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, CONNECT, PATCH
        Access-Control-Allow-Origin: *
        Access-Control-Max-Age: 3600
        Cache-control: private
        Content-Type: application/json
        [...]
        breadcrumbId: FE8FBEE819ADD3E-0000000000000001
        transfer-encoding: chunked

        {
            "status": "OK"
        }
        ```

    - `POST /webhook/amqpbridge` :

        - `OK` response:

            ```zsh
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
            ```zsh
            POST /webhook/amqpbridge HTTP/1.1
            Accept: application/json, */*;q=0.5
            [...]
            content-type: application/xml

            <?xml version="1.0" encoding="UTF-8"?>
            <event>
            [...]

            HTTP/1.1 200 OK
            Access-Control-Allow-Headers: Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers
            Access-Control-Allow-Methods: GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, CONNECT, PATCH
            Access-Control-Allow-Origin: *
            Access-Control-Max-Age: 3600
            Content-Type: application/json
            RHOAM_EVENT_ACTION: updated
            RHOAM_EVENT_TYPE: account
            [...]
            breadcrumbId: FE8FBEE819ADD3E-0000000000000002
            transfer-encoding: chunked

            {
                "status": "OK"
            }
            ```

        - `KO` response:

            ```zsh
            echo 'PLAIN TEXT' | http -v POST $URL/webhook/amqpbridge content-type:application/xml
            ```
            ```zsh
            POST /webhook/amqpbridge HTTP/1.1
            Accept: application/json, */*;q=0.5
            [...]
            User-Agent: HTTPie/2.4.0
            content-type: application/xml

            PLAIN TEXT

            HTTP/1.1 400 Bad Request
            Access-Control-Allow-Headers: Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers
            Access-Control-Allow-Methods: GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, CONNECT, PATCH
            Access-Control-Allow-Origin: *
            Access-Control-Max-Age: 3600
            Content-Type: application/json
            [..]
            breadcrumbId: C3AC19D2D0EC37E-0000000000000004
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
    ```zsh
    http -v $URL/openapi.json
    ```
    ```zsh
    GET /openapi.json HTTP/1.1
    Accept: */*
    [...]
    User-Agent: HTTPie/2.4.0

    HTTP/1.1 200 OK
    Accept: */*
    Accept-Encoding: gzip, deflate
    Access-Control-Allow-Headers: Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers
    Access-Control-Allow-Methods: GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, CONNECT, PATCH
    Access-Control-Allow-Origin: *
    Access-Control-Max-Age: 3600
    Cache-control: private
    Content-Type: application/vnd.oai.openapi+json
    [...]
    breadcrumbId: FE8FBEE819ADD3E-0000000000000004
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
                "url": "http://rhoam-webhook-events-handler-api.apps.jeannyil.sandbox438.opentlc.com"
            }
        ]
    }
    ```
4. Test the `/health` endpoint
    ```zsh
    curl -w '\n' $URL/health
    ```
    ```json
    {
        "status": "UP",
        "checks": [
            {
                "name": "camel-liveness-checks",
                "status": "UP"
            },
            {
                "name": "camel-readiness-checks",
                "status": "UP"
            },
            {
                "name": "camel-context-check",
                "status": "UP",
                "data": {
                    "contextStatus": "Started",
                    "name": "camel-1"
                }
            }
        ]
    }
    ```
5. Test the `/health/live` endpoint
    ```zsh
    curl -w '\n' $URL/health/live
    ```
    ```json
    {
        "status": "UP",
        "checks": [
            {
                "name": "camel-liveness-checks",
                "status": "UP"
            }
        ]
    }
    ```
6. Test the `/health/ready` endpoint
    ```zsh
    curl -w '\n' $URL/health/ready
    ```
    ```json
    {
        "status": "UP",
        "checks": [
            {
                "name": "camel-readiness-checks",
                "status": "UP"
            },
            {
                "name": "camel-context-check",
                "status": "UP",
                "data": {
                    "contextStatus": "Started",
                    "name": "camel-1"
                }
            }
        ]
    }
    ```
7. Test the `/metrics` endpoint
    ```zsh
    curl -w '\n' $URL/metrics
    ```
    ```zsh
    [...]
    # HELP application_camel_context_exchanges_total The total number of exchanges for a route or Camel Context
    # TYPE application_camel_context_exchanges_total counter
    application_camel_context_exchanges_total{camelContext="camel-1"} 20.0
    [...]
    # HELP application_camel_route_exchanges_total The total number of exchanges for a route or Camel Context
    # TYPE application_camel_route_exchanges_total counter
    application_camel_route_exchanges_total{camelContext="camel-1",routeId="common-500-http-code-route"} 0.0
    application_camel_route_exchanges_total{camelContext="camel-1",routeId="custom-http-error-route"} 0.0
    application_camel_route_exchanges_total{camelContext="camel-1",routeId="get-openapi-spec-route"} 2.0
    application_camel_route_exchanges_total{camelContext="camel-1",routeId="json-validation-api-route"} 20.0
    application_camel_route_exchanges_total{camelContext="camel-1",routeId="validate-membership-json-route"} 20.0
    [...]
    ```

## Creating a native executable

### Running locally

You can create a native executable using: `./mvnw package -Pnative`.

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: `./mvnw package -Pnative -Dquarkus.native.container-build=true`.

You can then execute your native executable with: `./target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/building-native-image.

### Deploying the native executable as an _OpenShift Serverless_ service

#### Prerequisites

- Access to a [Red Hat OpenShift](https://access.redhat.com/documentation/en-us/openshift_container_platform) 4 cluster
    - _[OpenShift Serverless](https://access.redhat.com/documentation/en-us/openshift_container_platform/4.6/html/serverless_applications/installing-openshift-serverless-1#installing-openshift-serverless)_ operator is installed
    - _[OpenShift Knative Serving](https://access.redhat.com/documentation/en-us/openshift_container_platform/4.6/html/serverless_applications/installing-openshift-serverless-1#installing-knative-serving)_ is installed
- _[Podman](https://podman.io/)_ or _[Docker](https://www.docker.com/)_ container-runtime environment for native compilation
- The _[kn](https://access.redhat.com/documentation/en-us/openshift_container_platform/4.7/html/serverless/installing-kn)_ CLI tool is installed
- User has self-provisioner privilege or has access to a working OpenShift project

#### Instructions

1. Login to the OpenShift cluster
    ```zsh
    oc login ...
    ```

2. Create an OpenShift project or use your existing OpenShift project. For instance, to create `camel-quarkus-native`
    ```zsh
    oc new-project camel-quarkus-native --display-name="Apache Camel Quarkus Apps - Native Mode"
    ```

3. Build a Linux executable using a container build. Compiling a Quarkus application to a native executable consumes a lot of memory during analysis and optimization. You can limit the amount of memory used during native compilation by setting the `quarkus.native.native-image-xmx` configuration property. Setting low memory limits might increase the build time.
    1. For Docker use:
        ```zsh
        ./mvnw package -Pnative -Dquarkus.native.container-build=true \
        -Dquarkus.native.builder-image=registry.access.redhat.com/quarkus/mandrel-20-rhel8:20.1 \
        -Dquarkus.native.native-image-xmx=6g
        ```
    2. For Podman use:
        ```zsh
        ./mvnw package -Pnative -Dquarkus.native.container-build=true \
        -Dquarkus.native.container-runtime=podman \
        -Dquarkus.native.builder-image=registry.access.redhat.com/quarkus/mandrel-20-rhel8:20.1 \
        -Dquarkus.native.native-image-xmx=7g
        ```
    ```zsh
    [...]
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildStep] Checking image status registry.access.redhat.com/quarkus/mandrel-20-rhel8:20.1
    20.1: Pulling from quarkus/mandrel-20-rhel8
    Digest: sha256:572668ceda75bed91d2b1d268a97511b1ebc8ce00a4ae668e9505c033f42fb60
    Status: Image is up to date for registry.access.redhat.com/quarkus/mandrel-20-rhel8:20.1
    registry.access.redhat.com/quarkus/mandrel-20-rhel8:20.1
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildStep] Running Quarkus native-image plugin on GraalVM Version 20.1.0.4_0-1 (Mandrel Distribution) (Java Version 11.0.10+9-LTS)
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildStep] docker run -v /Users/jeannyil/Workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-native-image-source-jar:/project:z --env LANG=C --rm registry.access.redhat.com/quarkus/mandrel-20-rhel8:20.1 -J-Dsun.nio.ch.maxUpdateArraySize=100 -J-Djava.util.logging.manager=org.jboss.logmanager.LogManager -J-Dvertx.logger-delegate-factory-class-name=io.quarkus.vertx.core.runtime.VertxLogDelegateFactory -J-Dvertx.disableDnsResolver=true -J-Dio.netty.leakDetection.level=DISABLED -J-Dio.netty.allocator.maxOrder=1 -J-Dcom.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize=true -J-Duser.language=en -J-Dfile.encoding=UTF-8 --initialize-at-build-time= -H:InitialCollectionPolicy=com.oracle.svm.core.genscavenge.CollectionPolicy\$BySpaceAndTime -H:+JNI -jar camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar -H:FallbackThreshold=0 -H:+ReportExceptionStackTraces -J-Xmx7g -H:+AddAllCharsets -H:EnableURLProtocols=http,https --enable-all-security-services -H:-UseServiceLoaderFeature -H:+StackTrace camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner
    [...]
    [INFO] [io.quarkus.deployment.QuarkusAugmentor] Quarkus augmentation completed in 1699198ms
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time:  28:32 min
    [INFO] Finished at: 2021-05-15T23:02:31+02:00
    [INFO] ------------------------------------------------------------------------
    ```

4. Create the `camel-quarkus-rhoam-webhook-handler-api` container image using the _OpenShift Docker build_ strategy. This strategy creates a container using a build configuration in the cluster.
    1. Create a build config based on the [`src/main/docker/Dockerfile.native`](./src/main/docker/Dockerfile.native) file:
        ```zsh
        cat src/main/docker/Dockerfile.native | oc new-build \
        --name camel-quarkus-rhoam-webhook-handler-api --strategy=docker --dockerfile -
        ```
    2. Build the project:
        ```zsh
        oc start-build camel-quarkus-rhoam-webhook-handler-api --from-dir . -F
        ```
        ```zsh
        Uploading directory "." as binary input for the build ...
        ........
        Uploading finished
        build.build.openshift.io/camel-quarkus-rhoam-webhook-handler-api-2 started
        Receiving source from STDIN as archive ...
        Replaced Dockerfile FROM image registry.access.redhat.com/ubi8/ubi-minimal:8.1
        Caching blobs under "/var/cache/blobs".
        [...]
        Successfully pushed image-registry.openshift-image-registry.svc:5000/camel-quarkus-native/camel-quarkus-rhoam-webhook-handler-api@sha256:e7a060a4a7070d661a8a2639bcc068919cd727fbc6d3e7bf5cb640ce3c91c6b5
        Push successful
        ```

5. Create the `quarkus-amqpbroker-connection-secret` containing the _QUARKUS QPID JMS_ [configuration options](https://github.com/amqphub/quarkus-qpid-jms#configuration). These options are leveraged by the _Camel Quarkus AMQP_ extension to connect to an AMQP broker. 
:warning: _Replace with values according to your AMQP broker environment_
    ```zsh
    oc create secret generic quarkus-amqpbroker-connection-secret \
    --from-literal=QUARKUS_QPID-JMS_URL="amqps://amq-ssl-broker-amqp-0-svc.amq7-broker-cluster.svc:5672?transport.trustAll=true&transport.verifyHost=false&amqp.idleTimeout=120000" \
    --from-literal=QUARKUS_QPID-JMS_USERNAME="amq-user" \
    --from-literal=QUARKUS_QPID-JMS_PASSWORD="P@ssw0rd"
    ```

6. Deploy the `camel-quarkus-rhoam-webhook-handler-api` as a serverless application.
    ```zsh
    kn service create camel-quarkus-rhoam-webhook-handler-api \
    --label app.openshift.io/runtime=quarkus \
    --env-from secret:quarkus-amqpbroker-connection-secret
    --image image-registry.openshift-image-registry.svc:5000/camel-quarkus-native/camel-quarkus-rhoam-webhook-handler-api:latest
    ```

7. To verify that the `camel-quarkus-rhoam-webhook-handler-api` service is ready, enter the following command.
    ```zsh
    kn service list camel-quarkus-rhoam-webhook-handler-api
    ```
    The output in the column called "READY" reads `True` if the service is ready.

## Start-up time comparison on the same OpenShift cluster

### JVM mode

```zsh
[...]
2021-05-15 12:49:24,464 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: send-to-amqp-queue-route started and consuming from: direct://sendToAMQPQueue
2021-05-15 12:49:24,464 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: ping-webhook-route started and consuming from: direct://pingWebhook
2021-05-15 12:49:24,465 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: generate-error-response-route started and consuming from: direct://generateErrorResponse
2021-05-15 12:49:24,469 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: get-openapi-spec-route started and consuming from: platform-http:///openapi.json
2021-05-15 12:49:24,470 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: webhook-amqpbridge-ping-route started and consuming from: platform-http:///webhook/amqpbridge
2021-05-15 12:49:24,470 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: webhook-amqpbridge-handler-route started and consuming from: platform-http:///webhook/amqpbridge
2021-05-15 12:49:24,471 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Total 6 routes, of which 6 are started
2021-05-15 12:49:24,471 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.4.2 (camel-1) started in 0.118 seconds
2021-05-15 12:49:24,546 INFO  [io.quarkus] (main) camel-quarkus-rhoam-webhook-handler-api 1.0.0 on JVM (powered by Quarkus 1.7.5.Final-redhat-00007) started in 1.215s. Listening on: http://0.0.0.0:8080
2021-05-15 12:49:24,547 INFO  [io.quarkus] (main) Profile prod activated.
[...]
```

### Native mode

```zsh
[...]
2021-05-15 21:18:33,882 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: send-to-amqp-queue-route started and consuming from: direct://sendToAMQPQueue
2021-05-15 21:18:33,882 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: ping-webhook-route started and consuming from: direct://pingWebhook
2021-05-15 21:18:33,882 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: generate-error-response-route started and consuming from: direct://generateErrorResponse
2021-05-15 21:18:33,882 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: get-openapi-spec-route started and consuming from: platform-http:///openapi.json
2021-05-15 21:18:33,883 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: webhook-amqpbridge-ping-route started and consuming from: platform-http:///webhook/amqpbridge
2021-05-15 21:18:33,883 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: webhook-amqpbridge-handler-route started and consuming from: platform-http:///webhook/amqpbridge
2021-05-15 21:18:33,883 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Total 6 routes, of which 6 are started
2021-05-15 21:18:33,883 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.4.2 (camel-1) started in 0.002 seconds
2021-05-15 21:18:33,904 INFO  [io.quarkus] (main) camel-quarkus-rhoam-webhook-handler-api 1.0.0 native (powered by Quarkus 1.7.5.Final-redhat-00007) started in 0.105s. Listening on: http://0.0.0.0:8080
2021-05-15 21:18:33,904 INFO  [io.quarkus] (main) Profile prod activated.
[...]
```


```zsh
jean-macbookair:~ # oc set env deployment/camel-quarkus-rhoam-webhook-handler-api \
QUARKUS_QPID-JMS_URL="amqps://amq-ssl-broker-amqp-0-svc.amq7-broker-cluster.svc:5672?transport.trustAll=true&transport.verifyHost=false&amqp.idleTimeout=120000" \
QUARKUS_QPID-JMS_USERNAME=amq-user \
QUARKUS_QPID-JMS_PASSWORD=P@ssw0rd
deployment.apps/camel-quarkus-rhoam-webhook-handler-api updated
```
```zsh
jean-macbookair:~ # oc set env dc/camel-quarkus-rhoam-webhook-handler-api \
QUARKUS_QPID-JMS_URL="amqps://amq-ssl-broker-amqp-0-svc.amq7-broker-cluster.svc:5672?transport.trustAll=true&transport.verifyHost=false&amqp.idleTimeout=120000" \
QUARKUS_QPID-JMS_USERNAME=amq-user \
QUARKUS_QPID-JMS_PASSWORD=P@ssw0rd
deploymentconfig.apps.openshift.io/camel-quarkus-rhoam-webhook-handler-api updated
jean-macbookair:~ # oc rollout latest dc/camel-quarkus-rhoam-webhook-handler-api
deploymentconfig.apps.openshift.io/camel-quarkus-rhoam-webhook-handler-api rolled out
```

```zsh
oc create secret generic quarkus-amqpbroker-connection-secret \
--from-literal=QUARKUS_QPID-JMS_URL="amqps://amq-ssl-broker-amqp-0-svc.amq7-broker-cluster.svc:5672?transport.trustAll=true&transport.verifyHost=false&amqp.idleTimeout=120000" \
--from-literal=QUARKUS_QPID-JMS_USERNAME=amq-user \
--from-literal=QUARKUS_QPID-JMS_PASSWORD=P@ssw0rd
```
```zsh
kn service update camel-quarkus-rhoam-webhook-handler-api \
--env-from secret:quarkus-amqpbroker-connection-secret \
--
```
