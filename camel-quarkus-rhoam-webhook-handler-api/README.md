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
- An [**AMQP 1.0 protocol**](https://www.amqp.org/) compliant broker should already be installed and running. [**Red Hat AMQ 7.10 broker on OpenShift**](https://access.redhat.com/documentation/en-us/red_hat_amq_broker/7.10) with an SSL-enabled AMQP acceptor has been used for testing.
- **OPTIONAL**: [**Jaeger**](https://www.jaegertracing.io/), a distributed tracing system for observability ([_open tracing_](https://opentracing.io/)). :bulb: A simple way of starting a Jaeger tracing server is with `docker` or `podman`:
    1. Start the Jaeger tracing server:
        ```
        podman run --rm -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 \
        -p 5775:5775/udp -p 6831:6831/udp -p 6832:6832/udp \
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
java -Dquarkus.kubernetes-config.enabled=false -Dintegrations-broker.url="amqps://amq-ssl-broker-amqp-0-svc-rte-amq7-broker-cluster.apps.jeannyil.sandbox1789.opentlc.com:443?transport.trustAll=true&transport.verifyHost=false&amqp.idleTimeout=120000" -Dquarkus.jaeger.endpoint="http://localhost:14268/api/traces" -jar target/quarkus-app/quarkus-run.jar
```

## Packaging and running the application on Red Hat OpenShift

### Pre-requisites
- Access to a [Red Hat OpenShift](https://access.redhat.com/documentation/en-us/openshift_container_platform) cluster v3 or v4
- User has self-provisioner privilege or has access to a working OpenShift project
- An [**AMQP 1.0 protocol**](https://www.amqp.org/) compliant broker should already be installed and running. [**Red Hat AMQ 7.8 broker on OpenShift**](https://access.redhat.com/documentation/en-us/red_hat_amq/2020.q4/html/deploying_amq_broker_on_openshift/index) with an SSL-enabled AMQP acceptor has been used for testing.

1. Login to the OpenShift cluster
    ```shell script
    oc login ...
    ```

2. Create an OpenShift project or use your existing OpenShift project. For instance, to create `oc get `
    ```shell script
    oc new-project camel-quarkus-jvm --display-name="Apache Camel Quarkus Apps - JVM Mode"
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
./mvnw clean package -Dquarkus.kubernetes.deploy=true
```
```shell script
[...]
INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Selecting target 'openshift' since it has the highest priority among the implicitly enabled deployment targets
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeploy] Kubernetes API Server at 'https://api.jeannyil.sandbox1789.opentlc.com:6443/' successfully contacted.
[INFO] Checking for existing resources in: /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/src/main/kubernetes.
[...]
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Performing openshift binary build with jar on server: https://api.jeannyil.sandbox1789.opentlc.com:6443/ in namespace:camel-quarkus-jvm.
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Applied: ImageStream camel-quarkus-rhoam-webhook-handler-api
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Applied: ImageStream openjdk-11
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Applied: BuildConfig camel-quarkus-rhoam-webhook-handler-api
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Receiving source from STDIN as archive ...
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Caching blobs under "/var/cache/blobs".
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] 
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Pulling image registry.access.redhat.com/ubi8/ubi-minimal:8.4 ...
[...]
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Pushing image image-registry.openshift-image-registry.svc:5000/camel-quarkus-jvm/camel-quarkus-rhoam-webhook-handler-api:1.0.0 ...
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Getting image source signatures
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:4418ace46c3dd933f98d83f357f31048e72d5db3d97bccfdb0acef769ee8234f
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:b4a4e359e27438bf3181a61aaa0dbe0ca8cc310a9d41f4470189170107ecff08
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:2a99c93da16827d9a6254f86f495d2c72c62a916f9c398577577221d35d2c790
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:1b71488243e23a6f95f7b63cb71aac4a579a3d5988e3d3a25c78bf12098591ea
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying config sha256:45517b22d34f7c43a01a391fe6c47c666a5fba371d4ca59d62e93882f25d97e0
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Writing manifest to image destination
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Storing signatures
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Successfully pushed image-registry.openshift-image-registry.svc:5000/camel-quarkus-jvm/camel-quarkus-rhoam-webhook-handler-api@sha256:9b6a9e4e423ddc47dc63a0b53880947d0e527f63b07bc423930c7274cc0be188
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Push successful
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Deploying to openshift server: https://api.jeannyil.sandbox1789.opentlc.com:6443/ in namespace: camel-quarkus-jvm.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: ServiceAccount camel-quarkus-rhoam-webhook-handler-api.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: Service camel-quarkus-rhoam-webhook-handler-api.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: Role view-secrets.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: RoleBinding camel-quarkus-rhoam-webhook-handler-api-view.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: RoleBinding camel-quarkus-rhoam-webhook-handler-api-view-secrets.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: ImageStream camel-quarkus-rhoam-webhook-handler-api.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: ImageStream openjdk-11.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: BuildConfig camel-quarkus-rhoam-webhook-handler-api.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: DeploymentConfig camel-quarkus-rhoam-webhook-handler-api.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: Route camel-quarkus-rhoam-webhook-handler-api.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] The deployed application can be accessed at: http://camel-quarkus-rhoam-webhook-handler-api-camel-quarkus-jvm.apps.jeannyil.sandbox1789.opentlc.com
[INFO] [io.quarkus.deployment.QuarkusAugmentor] Quarkus augmentation completed in 62362ms
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:10 min
[INFO] Finished at: 2021-11-20T16:01:38+01:00
[INFO] ------------------------------------------------------------------------
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
    oc create -f <(echo '
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
    Successfully pushed image-registry.openshift-image-registry.svc:5000/camel-quarkus-jvm/camel-quarkus-rhoam-webhook-handler-api@sha256:9932efcb67f775fcecef2055892c01ac337f64d7cc55f96197edda537536f424
    Push successful
    ```

5. Create a non-secure route to expose the `camel-quarkus-rhoam-webhook-handler-api` service outside the OpenShift cluster
    ```shell script
    oc expose svc/camel-quarkus-rhoam-webhook-handler-api
    ```

## Testing the application on OpenShift

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
- An [**AMQP 1.0 protocol**](https://www.amqp.org/) compliant broker should already be installed and running. [**Red Hat AMQ 7.8 broker on OpenShift**](https://access.redhat.com/documentation/en-us/red_hat_amq/2020.q4/html/deploying_amq_broker_on_openshift/index) with an SSL-enabled AMQP acceptor has been used for testing.

#### Instructions

1. Login to the OpenShift cluster
    ```shell script
    oc login ...
    ```

2. Create an OpenShift project or use your existing OpenShift project. For instance, to create `camel-quarkus-native`
    ```shell script
    oc new-project camel-quarkus-native --display-name="Apache Camel Quarkus Apps - Native Mode"
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

4. Create the `quarkus-amqpbroker-connection-secret` containing the _QUARKUS QPID JMS_ [configuration options](https://github.com/amqphub/quarkus-qpid-jms#configuration). These options are leveraged by the _Camel Quarkus AMQP_ extension to connect to an AMQP broker. 

    :warning: _Replace values with your AMQP broker environment_
    ```shell script
    oc create secret generic quarkus-amqpbroker-connection-secret \
    --from-literal=integrations-broker.url="amqps://<AMQP_HOST_CHANGE_ME>:<AMQP_PORT_CHANGE_ME>?transport.trustAll=true&transport.verifyHost=false&amqp.idleTimeout=120000" \
    --from-literal=integrations-broker.username=<CHANGE_ME> \
    --from-literal=integrations-broker.password=<CHANGE_ME> \
    --from-literal=integrations-broker.pool-max-connections=1 \
    --from-literal=integrations-broker.max-sessions-per-connection=500
    ```

5. Create the `quarkus-opentracing-endpoint-secret` containing the _QUARKUS OPENTRACING_ [endpoint configuration options](https://quarkus.io/version/main/guides/opentracing#configuration-reference). These options are leveraged by the _Camel Quarkus Opentracing_ extension to connect to the jaeger collector. Adapt the `quarkus.jaeger.endpoint`according to your environment.

    :warning: _Replace values with your AMQP broker environment_
    ```shell script
    oc create secret generic quarkus-opentracing-endpoint-secret \
    --from-literal=quarkus.jaeger.endpoint="http://jaeger-all-in-one-inmemory-collector.camel-quarkus-native.svc:14268/api/traces"
    ``` 

6. Build a Linux executable using a container build. Compiling a Quarkus application to a native executable consumes a lot of memory during analysis and optimization. You can limit the amount of memory used during native compilation by setting the `quarkus.native.native-image-xmx` configuration property. Setting low memory limits might increase the build time.
    1. For Docker use:
        ```shell script
        ./mvnw package -Pnative -Dquarkus.native.container-build=true \
        -Dquarkus.native.native-image-xmx=7g

        TODO -> ./mvnw clean package -Pnative -Dquarkus.kubernetes.deploy=true \
        -Dquarkus.native.native-image-xmx=7g
        ```
    2. For Podman use:
        ```shell script
        ./mvnw package -Pnative -Dquarkus.native.container-build=true \
        -Dquarkus.native.container-runtime=podman \
        -Dquarkus.native.native-image-xmx=7g

        TODO -> ./mvnw clean package -Pnative -Dquarkus.kubernetes.deploy=true \
        -Dquarkus.native.container-runtime=podman \
        -Dquarkus.native.native-image-xmx=7g
        ```
    ```shell script
    [...]
    [INFO] [io.quarkus.deployment.pkg.steps.JarResultBuildStep] Building native image source jar: /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-native-image-source-jar/camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildStep] Building native image from /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-native-image-source-jar/camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildContainerRunner] Using docker to run the native image builder
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildContainerRunner] Checking image status registry.access.redhat.com/quarkus/mandrel-21-rhel8:21.2
    21.2: Pulling from quarkus/mandrel-21-rhel8
    ce3c6836540f: Pull complete 
    63f9f4c31162: Pull complete 
    9214a6f2b567: Pull complete 
    Digest: sha256:7527acf6db5c01225cd208326a08c057fe506fa61a72cd67b503d372214981a0
    Status: Downloaded newer image for registry.access.redhat.com/quarkus/mandrel-21-rhel8:21.2
    registry.access.redhat.com/quarkus/mandrel-21-rhel8:21.2
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildStep] Running Quarkus native-image plugin on native-image 21.2.0.2-0b3 Mandrel Distribution (Java Version 11.0.13+8-LTS)
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildRunner] docker run --env LANG=C --rm -v /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-native-image-source-jar:/project:z --name build-native-cvdBL registry.access.redhat.com/quarkus/mandrel-21-rhel8:21.2 -J-Djava.util.logging.manager=org.jboss.logmanager.LogManager -J-Dsun.nio.ch.maxUpdateArraySize=100 -J-Dcom.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize=true -J-Dvertx.logger-delegate-factory-class-name=io.quarkus.vertx.core.runtime.VertxLogDelegateFactory -J-Dvertx.disableDnsResolver=true -J-Dio.netty.leakDetection.level=DISABLED -J-Dio.netty.allocator.maxOrder=3 -J-Duser.language=en -J-Duser.country=FR -J-Dfile.encoding=UTF-8 -H:InitialCollectionPolicy=com.oracle.svm.core.genscavenge.CollectionPolicy\$BySpaceAndTime -H:+JNI -H:+AllowFoldMethods -H:FallbackThreshold=0 -H:+ReportExceptionStackTraces -J-Xmx7g -H:+AddAllCharsets -H:EnableURLProtocols=http,https -H:-UseServiceLoaderFeature -H:+StackTrace -H:-ParseOnce camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner -jar camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar
    [...]
    [INFO] Checking for existing resources in: /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/src/main/kubernetes.
    [INFO] [io.quarkus.deployment.QuarkusAugmentor] Quarkus augmentation completed in 577080ms
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time:  09:45 min
    [INFO] Finished at: 2021-11-24T11:52:32+01:00
    [INFO] ------------------------------------------------------------------------

    TODO: REPLACE WITH THIS BELOW
    [...]
    [INFO] [io.quarkus.deployment.pkg.steps.JarResultBuildStep] Building native image source jar: /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-native-image-source-jar/camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildStep] Building native image from /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-native-image-source-jar/camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildContainerRunner] Using docker to run the native image builder
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildContainerRunner] Checking image status registry.access.redhat.com/quarkus/mandrel-21-rhel8:21.2
    21.2: Pulling from quarkus/mandrel-21-rhel8
    Digest: sha256:7527acf6db5c01225cd208326a08c057fe506fa61a72cd67b503d372214981a0
    Status: Image is up to date for registry.access.redhat.com/quarkus/mandrel-21-rhel8:21.2
    registry.access.redhat.com/quarkus/mandrel-21-rhel8:21.2
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildStep] Running Quarkus native-image plugin on native-image 21.2.0.2-0b3 Mandrel Distribution (Java Version 11.0.13+8-LTS)
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildRunner] docker run --env LANG=C --rm -v /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-native-image-source-jar:/project:z --name build-native-epNae registry.access.redhat.com/quarkus/mandrel-21-rhel8:21.2 -J-Dsun.nio.ch.maxUpdateArraySize=100 -J-Djava.util.logging.manager=org.jboss.logmanager.LogManager -J-Dcom.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize=true -J-Dvertx.logger-delegate-factory-class-name=io.quarkus.vertx.core.runtime.VertxLogDelegateFactory -J-Dvertx.disableDnsResolver=true -J-Dio.netty.leakDetection.level=DISABLED -J-Dio.netty.allocator.maxOrder=3 -J-Duser.language=en -J-Duser.country=FR -J-Dfile.encoding=UTF-8 -H:InitialCollectionPolicy=com.oracle.svm.core.genscavenge.CollectionPolicy\$BySpaceAndTime -H:+JNI -H:+AllowFoldMethods -H:FallbackThreshold=0 -H:+ReportExceptionStackTraces -J-Xmx7g -H:+AddAllCharsets -H:EnableURLProtocols=http,https -H:-UseServiceLoaderFeature -H:+StackTrace -H:-ParseOnce camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner -jar camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar
    [...]
    # Printing build artifacts to: /project/camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.build_artifacts.txt
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildRunner] docker run --env LANG=C --rm -v /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-native-image-source-jar:/project:z --entrypoint /bin/bash registry.access.redhat.com/quarkus/mandrel-21-rhel8:21.2 -c objcopy --strip-debug camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner
    [INFO] Checking for existing resources in: /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/src/main/kubernetes.
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Performing openshift binary build with native image on server: https://api.eannyil.sandbox1789.opentlc.com:6443/ in namespace:camel-quarkus-native.
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Applied: ImageStream camel-quarkus-rhoam-webhook-handler-api
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Found: ImageStream s2i-java repository: fabric8/s2i-java
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Applied: BuildConfig camel-quarkus-rhoam-webhook-handler-api
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Receiving source from STDIN as archive ...
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Caching blobs under "/var/cache/blobs".
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] 
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Pulling image registry.access.redhat.com/ubi8/ubi-minimal:8.4 ...
    [...]
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Pushing image image-registry.openshift-image-registry.svc:5000/camel-quarkus-native/camel-quarkus-rhoam-webhook-handler-api:1.0.0 ...
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Getting image source signatures
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:878307b435168156c2b2b4a0e4bd803e7c6b8e57396b2cd873ca1a3bd85032c6
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:3b56abbadd1de32068d241d52c4e200acb509c34ee1bbb1f3c0e50f5f4a5e655
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:d46336f50433ab27336fad8f9b251b2f68a66d376c902dfca23a6851acae502c
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:be961ec6866344c06fe85e53011321da508bc495513bb75a45fc41f6182921b6
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying config sha256:37a60e1e85ea032efb576bf130e7346ebcd0d94d1636b7ad785cf5845018448d
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Writing manifest to image destination
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Storing signatures
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Successfully pushed image-registry.openshift-image-registry.svc:5000/camel-quarkus-native/camel-quarkus-rhoam-webhook-handler-api@sha256:b67d9b9e33910809bcfeceeebf14396fea959fa8435e59bc1b585f5d6c4e309c
    [INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Push successful
    [INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Deploying to openshift server: https://api.eannyil.sandbox1789.opentlc.com:6443/ in namespace: camel-quarkus-native.
    [INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: ServiceAccount camel-quarkus-rhoam-webhook-handler-api.
    [INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: Service camel-quarkus-rhoam-webhook-handler-api.
    [INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: Role view-secrets.
    [INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: RoleBinding camel-quarkus-rhoam-webhook-handler-api-view.
    [INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: RoleBinding camel-quarkus-rhoam-webhook-handler-api-view-secrets.
    [INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: ImageStream camel-quarkus-rhoam-webhook-handler-api.
    [INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: ImageStream s2i-java.
    [INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: BuildConfig camel-quarkus-rhoam-webhook-handler-api.
    [INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: DeploymentConfig camel-quarkus-rhoam-webhook-handler-api.
    [INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: Route camel-quarkus-rhoam-webhook-handler-api.
    [INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] The deployed application can be accessed at: http://camel-quarkus-rhoam-webhook-handler-api-camel-quarkus-native.apps.eannyil.sandbox1789.opentlc.com
    [INFO] [io.quarkus.deployment.QuarkusAugmentor] Quarkus augmentation completed in 1131420ms
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time:  19:01 min
    [INFO] Finished at: 2021-11-27T16:38:55+01:00
    [INFO] ------------------------------------------------------------------------
    ```

7. Create the `camel-quarkus-rhoam-webhook-handler-api` container image using the _OpenShift Docker build_ strategy. This strategy creates a container using a build configuration in the cluster.
    1. Create a build config based on the [`src/main/docker/Dockerfile.native`](./src/main/docker/Dockerfile.native) file:
        ```shell script
        cat src/main/docker/Dockerfile.native | oc new-build \
        --name camel-quarkus-rhoam-webhook-handler-api --strategy=docker --dockerfile -
        ```
    2. Build the project:
        ```shell script
        oc start-build camel-quarkus-rhoam-webhook-handler-api --from-dir . -F
        ```
        ```shell script
        Uploading directory "." as binary input for the build ...
        .....
        Uploading finished
        build.build.openshift.io/camel-quarkus-rhoam-webhook-handler-api-2 started
        Receiving source from STDIN as archive ...
        Replaced Dockerfile FROM image registry.access.redhat.com/ubi8/ubi-minimal:8.4
        Caching blobs under "/var/cache/blobs".
        [...]
        Pushing image image-registry.openshift-image-registry.svc:5000/camel-quarkus-native/camel-quarkus-rhoam-webhook-handler-api:latest ...
        [...]
        Successfully pushed image-registry.openshift-image-registry.svc:5000/camel-quarkus-native/camel-quarkus-rhoam-webhook-handler-api@sha256:b4fa834c6f47e183f960a2df13a33b62713ce8f00df5b5774459377396504424
        Push successful
        ```

8. Create the `camel-quarkus-rhoam-webhook-handler-api` service account that will be used to run the quarkus application.
    ```shell script
    oc create -f <(echo '
    apiVersion: v1
    kind: ServiceAccount
    metadata:
      labels:
        app.openshift.io/runtime: quarkus
        app.kubernetes.io/name: camel-quarkus-rhoam-webhook-handler-api
        app.kubernetes.io/version: 1.0.0
      name: camel-quarkus-rhoam-webhook-handler-api
    ')
    ```

9. Create the `view-secrets` role and bind it, along with the `view` cluster role, to the `camel-quarkus-rhoam-webhook-handler-api` service account used to run the quarkus application. These permissions allow the `camel-quarkus-rhoam-webhook-handler-api` service account to access secrets.
    ```shell script
    oc create -f <(echo '
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
      name: camel-quarkus-rhoam-webhook-handler-api-view
    roleRef:
      kind: ClusterRole
      apiGroup: rbac.authorization.k8s.io
      name: view
    subjects:
      - kind: ServiceAccount
        name: camel-quarkus-rhoam-webhook-handler-api
    ---
    apiVersion: rbac.authorization.k8s.io/v1
    kind: RoleBinding
    metadata:
      name: camel-quarkus-rhoam-webhook-handler-api-view-secrets
    roleRef:
      kind: Role
      apiGroup: rbac.authorization.k8s.io
      name: view-secrets
    subjects:
      - kind: ServiceAccount
        name: camel-quarkus-rhoam-webhook-handler-api
    ')
    ```

10. Deploy the `camel-quarkus-rhoam-webhook-handler-api` as a serverless application.
    ```shell script
    kn service create camel-quarkus-rhoam-webhook-handler-api \
    --label app.openshift.io/runtime=quarkus \
    --service-account camel-quarkus-rhoam-webhook-handler-api \
    --image image-registry.openshift-image-registry.svc:5000/camel-quarkus-native/camel-quarkus-rhoam-webhook-handler-api:latest
    ```

11. To verify that the `camel-quarkus-rhoam-webhook-handler-api` service is ready, enter the following command.
    ```shell script
    kn service list camel-quarkus-rhoam-webhook-handler-api
    ```
    The output in the column called "READY" reads `True` if the service is ready.

## TODO : serverless

```shell script
./mvnw clean package -Dquarkus.kubernetes.deploy=true \
-Dquarkus.kubernetes.deployment-target=knative \
-Dquarkus.container-image.registry=image-registry.openshift-image-registry.svc:5000 \
-Dquarkus.container-image.group=test-serverless
```
```shell script
[...]
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeploy] Kubernetes API Server at 'https://api.jeannyil.sandbox1789.opentlc.com:6443/' successfully contacted.
[INFO] Checking for existing resources in: /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/src/main/kubernetes.
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Performing openshift binary build with jar on server: https://api.jeannyil.sandbox1789.opentlc.com:6443/ in namespace:test-serverless.
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Applied: ImageStream camel-quarkus-rhoam-webhook-handler-api
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Applied: ImageStream openjdk-11
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Applied: BuildConfig camel-quarkus-rhoam-webhook-handler-api
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Receiving source from STDIN as archive ...
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Caching blobs under "/var/cache/blobs".
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] 
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Pulling image registry.access.redhat.com/ubi8/ubi-minimal:8.4 ...
[...]
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Pushing image image-registry.openshift-image-registry.svc:5000/test-serverless/camel-quarkus-rhoam-webhook-handler-api:1.0.0 ...
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Getting image source signatures
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:a02d1d040deb4ab4db5d5a536745734224b807ec2cd6fbfab0a40f1c462013a7
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:fb1e3ac9e8de332e8f81b73d8e31cc53c1d90db23b1dc9a9adea9466dd6eaf25
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:d1095dca481ce44dc8c2e681f081078365cc1fa3408e68e90509621b22697f0f
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:59d6a097139015dd7eff86f5397791571c4013bdadc4632572d143ecd111c612
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:d46336f50433ab27336fad8f9b251b2f68a66d376c902dfca23a6851acae502c
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:be961ec6866344c06fe85e53011321da508bc495513bb75a45fc41f6182921b6
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:cb6c74f325620a34f22dd4e35243939c34103423c1af1c1479fbe2701c0eb0a7
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying config sha256:b1f6b4cad23ae18c29df8231969b9387f681ad4ee0779151137fe3ae6b6060e4
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Writing manifest to image destination
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Storing signatures
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Successfully pushed image-registry.openshift-image-registry.svc:5000/test-serverless/camel-quarkus-rhoam-webhook-handler-api@sha256:9b3a3d9efe852b9c58deedc6612c92695319baae3e96e36c877df0d6b7f8aab7
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Push successful
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Deploying to knative server: https://api.jeannyil.sandbox1789.opentlc.com:6443/ in namespace: test-serverless.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: ServiceAccount camel-quarkus-rhoam-webhook-handler-api.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: Service camel-quarkus-rhoam-webhook-handler-api.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: Role view-secrets.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: RoleBinding camel-quarkus-rhoam-webhook-handler-api-view.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: RoleBinding camel-quarkus-rhoam-webhook-handler-api-view-secrets.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: ConfigMap config-autoscaler.
[INFO] [io.quarkus.deployment.QuarkusAugmentor] Quarkus augmentation completed in 94998ms
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:44 min
[INFO] Finished at: 2021-11-27T21:50:13+01:00
[INFO] ------------------------------------------------------------------------
```

```shell script
./mvnw clean package -Pnative -Dquarkus.kubernetes.deploy=true \
-Dquarkus.kubernetes.deployment-target=knative \
-Dquarkus.container-image.registry=image-registry.openshift-image-registry.svc:5000 \
-Dquarkus.container-image.group=test-serverless \
-Dquarkus.native.native-image-xmx=7g
```
```shell script
[...]
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeploy] Kubernetes API Server at 'https://api.jeannyil.sandbox1789.opentlc.com:6443/' successfully contacted.
[INFO] [io.quarkus.deployment.pkg.steps.JarResultBuildStep] Building native image source jar: /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-native-image-source-jar/camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar
[INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildStep] Building native image from /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-native-image-source-jar/camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar
[INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildContainerRunner] Using docker to run the native image builder
[INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildContainerRunner] Checking image status registry.access.redhat.com/quarkus/mandrel-21-rhel8:21.2
21.2: Pulling from quarkus/mandrel-21-rhel8
Digest: sha256:7527acf6db5c01225cd208326a08c057fe506fa61a72cd67b503d372214981a0
Status: Image is up to date for registry.access.redhat.com/quarkus/mandrel-21-rhel8:21.2
registry.access.redhat.com/quarkus/mandrel-21-rhel8:21.2
[INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildStep] Running Quarkus native-image plugin on native-image 21.2.0.2-0b3 Mandrel Distribution (Java Version 11.0.13+8-LTS)
[INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildRunner] docker run --env LANG=C --rm -v /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-native-image-source-jar:/project:z --name build-native-sYbVE registry.access.redhat.com/quarkus/mandrel-21-rhel8:21.2 -J-Djava.util.logging.manager=org.jboss.logmanager.LogManager -J-Dsun.nio.ch.maxUpdateArraySize=100 -J-Dcom.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize=true -J-Dvertx.logger-delegate-factory-class-name=io.quarkus.vertx.core.runtime.VertxLogDelegateFactory -J-Dvertx.disableDnsResolver=true -J-Dio.netty.leakDetection.level=DISABLED -J-Dio.netty.allocator.maxOrder=3 -J-Duser.language=en -J-Duser.country=FR -J-Dfile.encoding=UTF-8 -H:InitialCollectionPolicy=com.oracle.svm.core.genscavenge.CollectionPolicy\$BySpaceAndTime -H:+JNI -H:+AllowFoldMethods -H:FallbackThreshold=0 -H:+ReportExceptionStackTraces -J-Xmx7g -H:+AddAllCharsets -H:EnableURLProtocols=http,https -H:-UseServiceLoaderFeature -H:+StackTrace -H:-ParseOnce camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner -jar camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar
[...]
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Performing openshift binary build with native image on server: https://api.jeannyil.sandbox1789.opentlc.com:6443/ in namespace:test-serverless.
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Applied: ImageStream camel-quarkus-rhoam-webhook-handler-api
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Applied: ImageStream s2i-java
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Applied: BuildConfig camel-quarkus-rhoam-webhook-handler-api
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Receiving source from STDIN as archive ...
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Caching blobs under "/var/cache/blobs".
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] 
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Pulling image registry.access.redhat.com/ubi8/ubi-minimal:8.4 ...
[...]
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Pushing image image-registry.openshift-image-registry.svc:5000/test-serverless/camel-quarkus-rhoam-webhook-handler-api:1.0.0 ...
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Getting image source signatures
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:13532d8fa0d604ed1c307328d8ee128f34f9d3347377bf3c73dd831de24a543c
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:1f8bf1c4b819ee0739be1fe7939e5969f217005333eaf28b984b5ad525acc574
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:be961ec6866344c06fe85e53011321da508bc495513bb75a45fc41f6182921b6
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying blob sha256:d46336f50433ab27336fad8f9b251b2f68a66d376c902dfca23a6851acae502c
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Copying config sha256:ce5a4892e1042f89a0767f366d9683dda2de28635b9d86d4eace91d9d55dc65c
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Writing manifest to image destination
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Storing signatures
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Successfully pushed image-registry.openshift-image-registry.svc:5000/test-serverless/camel-quarkus-rhoam-webhook-handler-api@sha256:e18ca19662e7bdb09a071cfc7c2017c343f25679e7db2a8af425e8f7db7a62be
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Push successful
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Deploying to knative server: https://api.jeannyil.sandbox1789.opentlc.com:6443/ in namespace: test-serverless.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: ServiceAccount camel-quarkus-rhoam-webhook-handler-api.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: Service camel-quarkus-rhoam-webhook-handler-api.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: Role view-secrets.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: RoleBinding camel-quarkus-rhoam-webhook-handler-api-view.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: RoleBinding camel-quarkus-rhoam-webhook-handler-api-view-secrets.
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Applied: ConfigMap config-autoscaler.
[INFO] [io.quarkus.deployment.QuarkusAugmentor] Quarkus augmentation completed in 1478794ms
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  25:08 min
[INFO] Finished at: 2021-11-27T22:27:30+01:00
[INFO] ------------------------------------------------------------------------
```

## Start-up time comparison on the same OpenShift cluster

- OpenShift Container Platform 4.8.19 running on AWS
- Compute nodes types: [m5.xlarge](https://aws.amazon.com/ec2/instance-types/m5/) (4 vCPU / 16 GiB Memory)

### JVM mode - 8.885s

```shell script
2021-11-24 13:26:44,452 INFO  [org.apa.cam.qua.cor.CamelBootstrapRecorder] (main) Bootstrap runtime: org.apache.camel.quarkus.main.CamelMainRuntime
2021-11-24 13:26:44,655 INFO  [org.apa.cam.mai.BaseMainSupport] (main) Auto-configuration summary
2021-11-24 13:26:44,656 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     camel.context.name=camel-quarkus-rhoam-webhook-handler-api
2021-11-24 13:26:45,538 INFO  [org.apa.cam.lan.xpa.XPathBuilder] (main) Created default XPathFactory org.apache.xpath.jaxp.XPathFactoryImpl@261b9a37
2021-11-24 13:26:45,751 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) StreamCaching is enabled on CamelContext: camel-quarkus-rhoam-webhook-handler-api
2021-11-24 13:26:45,839 INFO  [org.apa.cam.imp.eng.DefaultStreamCachingStrategy] (main) StreamCaching in use with spool directory: /tmp/camel/camel-tmp-B4A530A6317EE0F-0000000000000000 and rules: [Spool > 128K body size]
2021-11-24 13:26:45,850 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Routes startup summary (total:6 started:6)
2021-11-24 13:26:45,850 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started generate-error-response-route (direct://generateErrorResponse)
2021-11-24 13:26:45,850 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started send-to-amqp-queue-route (direct://sendToAMQPQueue)
2021-11-24 13:26:45,850 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started ping-webhook-route (direct://pingWebhook)
2021-11-24 13:26:45,850 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started get-openapi-spec-route (rest://get:/openapi.json)
2021-11-24 13:26:45,850 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started webhook-amqpbridge-ping-route (rest://get:/webhook/amqpbridge)
2021-11-24 13:26:45,850 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started webhook-amqpbridge-handler-route (rest://post:/webhook/amqpbridge)
2021-11-24 13:26:45,851 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.11.1 (camel-quarkus-rhoam-webhook-handler-api) started in 810ms (build:0ms init:711ms start:99ms)
2021-11-24 13:26:46,244 INFO  [io.quarkus] (main) camel-quarkus-rhoam-webhook-handler-api 1.0.0 on JVM (powered by Quarkus 2.2.3.Final-redhat-00013) started in 8.885s. Listening on: http://0.0.0.0:8080
2021-11-24 13:26:46,245 INFO  [io.quarkus] (main) Profile prod activated.
2021-11-24 13:26:46,245 INFO  [io.quarkus] (main) Installed features: [camel-amqp, camel-attachments, camel-bean, camel-core, camel-direct, camel-jackson, camel-jms, camel-microprofile-health, camel-microprofile-metrics, camel-openapi-java, camel-opentracing, camel-platform-http, camel-rest, camel-xml-jaxb, camel-xpath, cdi, config-yaml, jaeger, kubernetes, kubernetes-client, qpid-jms, smallrye-context-propagation, smallrye-health, smallrye-metrics, smallrye-opentracing, vertx, vertx-web]
```

### Native mode - 0.104s

```shell script
2021-11-24 13:28:40,517 INFO  [org.apa.cam.qua.cor.CamelBootstrapRecorder] (main) Bootstrap runtime: org.apache.camel.quarkus.main.CamelMainRuntime
2021-11-24 13:28:40,520 INFO  [org.apa.cam.mai.BaseMainSupport] (main) Auto-configuration summary
2021-11-24 13:28:40,520 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     camel.context.name=camel-quarkus-rhoam-webhook-handler-api
2021-11-24 13:28:40,525 INFO  [org.apa.cam.lan.xpa.XPathBuilder] (main) Created default XPathFactory com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl@36a3e2cf
2021-11-24 13:28:40,528 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) StreamCaching is enabled on CamelContext: camel-quarkus-rhoam-webhook-handler-api
2021-11-24 13:28:40,530 INFO  [org.apa.cam.imp.eng.DefaultStreamCachingStrategy] (main) StreamCaching in use with spool directory: /tmp/camel/camel-tmp-043E4AA14FF070D-0000000000000000 and rules: [Spool > 128K body size]
2021-11-24 13:28:40,531 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Routes startup summary (total:6 started:6)
2021-11-24 13:28:40,531 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started ping-webhook-route (direct://pingWebhook)
2021-11-24 13:28:40,531 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started send-to-amqp-queue-route (direct://sendToAMQPQueue)
2021-11-24 13:28:40,531 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started generate-error-response-route (direct://generateErrorResponse)
2021-11-24 13:28:40,531 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started get-openapi-spec-route (rest://get:/openapi.json)
2021-11-24 13:28:40,531 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started webhook-amqpbridge-ping-route (rest://get:/webhook/amqpbridge)
2021-11-24 13:28:40,531 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started webhook-amqpbridge-handler-route (rest://post:/webhook/amqpbridge)
2021-11-24 13:28:40,531 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.11.1 (camel-quarkus-rhoam-webhook-handler-api) started in 10ms (build:0ms init:7ms start:3ms)
2021-11-24 13:28:40,533 INFO  [io.quarkus] (main) camel-quarkus-rhoam-webhook-handler-api 1.0.0 native (powered by Quarkus 2.2.3.Final-redhat-00013) started in 0.104s. Listening on: http://0.0.0.0:8080
2021-11-24 13:28:40,533 INFO  [io.quarkus] (main) Profile prod activated.
2021-11-24 13:28:40,533 INFO  [io.quarkus] (main) Installed features: [camel-amqp, camel-attachments, camel-bean, camel-core, camel-direct, camel-jackson, camel-jms, camel-microprofile-health, camel-microprofile-metrics, camel-openapi-java, camel-opentracing, camel-platform-http, camel-rest, camel-xml-jaxb, camel-xpath, cdi, config-yaml, jaeger, kubernetes, kubernetes-client, qpid-jms, smallrye-context-propagation, smallrye-health, smallrye-metrics, smallrye-opentracing, vertx, vertx-web]
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