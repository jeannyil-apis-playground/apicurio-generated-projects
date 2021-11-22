# Camel-Quarkus-RHOAM-Webhook-Handler-Api project

This project leverages **Red Hat build of Quarkus 2.2.x**, the Supersonic Subatomic Java Framework.

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
data:
  quarkus.qpid-jms.password: UEBzc3cwcmQ=
  quarkus.qpid-jms.url: YW1xcHM6Ly9hbXEtc3NsLWJyb2tlci1hbXFwLTAtc3ZjLmFtcTctYnJva2VyLWNsdXN0ZXIuc3ZjOjU2NzI/dHJhbnNwb3J0LnRydXN0QWxsPXRydWUmdHJhbnNwb3J0LnZlcmlmeUhvc3Q9ZmFsc2UmYW1xcC5pZGxlVGltZW91dD0xMjAwMDA=
  quarkus.qpid-jms.username: YW1xLXVzZXI=
type: Opaque
```

## Prerequisites
- JDK 11 installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.8.1+
- An [**AMQP 1.0 protocol**](https://www.amqp.org/) compliant broker should already be installed and running. [**Red Hat AMQ 7.8 broker on OpenShift**](https://access.redhat.com/documentation/en-us/red_hat_amq/2020.q4/html/deploying_amq_broker_on_openshift/index) with an SSL-enabled AMQP acceptor has been used for testing.
- **OPTIONAL**: [**Jaeger**](https://www.jaegertracing.io/), a distributed tracing system for observability ([_open tracing_](https://opentracing.io/)). :warning: **TODO**:
    ```
    podman run --rm -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 \
    -p 5775:5775/udp -p 6831:6831/udp -p 6832:6832/udp \
    -p 5778:5778 -p 16686:16686 -p 14268:14268 -p 9411:9411 \
    quay.io/jaegertracing/all-in-one:latest
    ```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```
./mvnw quarkus:dev -Dquarkus.kubernetes-config.enabled=false
```

## Packaging and running the application locally

The application can be packaged using:
```zsh
./mvnw clean package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using:
```zsh
java -jar target/quarkus-app/quarkus-run.jar -Dquarkus.kubernetes-config.enabled=false
```

If you want to build an _über-jar_, execute the following command:
```zsh
./mvnw clean package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using:
```zsh
java -jar target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar -Dquarkus.kubernetes-config.enabled=false
```

According to your environment, you may want to customize:
- The [**AMQP broker connection parameters**](https://github.com/amqphub/quarkus-qpid-jms#configuration)  by adding the following run-time _system properties_:
    - `quarkus.qpid-jms.url`
    - `quarkus.qpid-jms.username`
    - `quarkus.qpid-jms.password`
- The Jaeger collector endpoint by adding the following run-time _system properties_:
    - `quarkus.jaeger.endpoint`

Example:
```
java -jar target/camel-quarkus-rhoam-webhook-handler-api-1.0.0.jar -Dquarkus.kubernetes-config.enabled=false -Dquarkus.qpid-jms.url="amqps://amq-ssl-broker-amqp-0-svc-rte-amq7-broker-cluster.apps.jeannyil.sandbox1789.opentlc.com:443?transport.trustAll=true&transport.verifyHost=false&amqp.idleTimeout=120000" -Dquarkus.jaeger.endpoint="http://localhost:14268/api/traces"
```

## Packaging and running the application on Red Hat OpenShift

### Pre-requisites
- Access to a [Red Hat OpenShift](https://access.redhat.com/documentation/en-us/openshift_container_platform) cluster v3 or v4
- User has self-provisioner privilege or has access to a working OpenShift project
- An [**AMQP 1.0 protocol**](https://www.amqp.org/) compliant broker should already be installed and running. [**Red Hat AMQ 7.8 broker on OpenShift**](https://access.redhat.com/documentation/en-us/red_hat_amq/2020.q4/html/deploying_amq_broker_on_openshift/index) with an SSL-enabled AMQP acceptor has been used for testing.

1. Login to the OpenShift cluster
    ```zsh
    oc login ...
    ```

2. Create an OpenShift project or use your existing OpenShift project. For instance, to create `camel-quarkus`
    ```zsh
    oc new-project camel-quarkus-jvm --display-name="Apache Camel Quarkus Apps - JVM Mode"
    ```
        
3. Create an `allInOne` Jaeger instance.
    1. **IF NOT ALREADY INSTALLED**:
        1. Install, via OLM, the `Red Hat OpenShift distributed tracing platform` (Jaeger) operator with an `AllNamespaces` scope. :warning: Needs `cluster-admin` privileges
            ```zsh
            oc create --save-config -f - <<EOF
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
            ```zsh
            watch oc get sub,csv
            ```
    2. Create the `allInOne` Jaeger instance.
        ```zsh
        oc create --save-config -f - <<EOF
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
    ```zsh
    oc create secret generic quarkus-amqpbroker-connection-secret \
    --from-literal=quarkus.qpid-jms.url="amqps://<AMQP_HOST_CHANGE_ME>:<AMQP_PORT_CHANGE_ME>?transport.trustAll=true&transport.verifyHost=false&amqp.idleTimeout=120000" \
    --from-literal=quarkus.qpid-jms.username=<CHANGE_ME> \
    --from-literal=quarkus.qpid-jms.password=<CHANGE_ME>
    ```

5. Create the `quarkus-opentracing-endpoint-secret` containing the _QUARKUS OPENTRACING_ [endpoint configuration options](https://quarkus.io/version/main/guides/opentracing#configuration-reference). These options are leveraged by the _Camel Quarkus Opentracing_ extension to connect to the jaeger collector. Adapt the `quarkus.jaeger.endpoint`according to your environment.

    :warning: _Replace values with your AMQP broker environment_
    ```zsh
    oc create secret generic quarkus-opentracing-endpoint-secret \
    --from-literal=quarkus.jaeger.endpoint="http://jaeger-all-in-one-inmemory-collector.camel-quarkus-jvm.svc:14268/api/traces"
    ```

6. Use either the _**S2I binary workflow**_ or _**S2I source workflow**_ to deploy the `camel-quarkus-rhoam-webhook-handler-api` app as described below.

### OpenShift S2I binary workflow 

This leverages the **Quarkus OpenShift** extension and is only recommended for development and testing purposes.

Make sure the latest supported OpenJDK 11 image is imported in OpenShift
```zsh
oc import-image --confirm openjdk-11-ubi8 \
--from=registry.access.redhat.com/ubi8/openjdk-11 \
-n openshift
```
```zsh
./mvnw clean package -Dquarkus.kubernetes.deploy=true
```
```zsh
[...]
INFO] [io.quarkus.kubernetes.deployment.KubernetesDeployer] Selecting target 'openshift' since it has the highest priority among the implicitly enabled deployment targets
[INFO] [io.quarkus.kubernetes.deployment.KubernetesDeploy] Kubernetes API Server at 'https://api.jeannyil.sandbox1789.opentlc.com:6443/' successfully contacted.
[INFO] Checking for existing resources in: /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/src/main/kubernetes.
[...]
[INFO] [io.quarkus.container.image.openshift.deployment.OpenshiftProcessor] Performing openshift binary build with jar on server: https://api.jeannyil.sandbox1789.opentlc.com:6443/ in namespace:camel-quarkus-jvm.
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
    ```zsh
    oc import-image --confirm openjdk-11-ubi8 \
    --from=registry.access.redhat.com/ubi8/openjdk-11 \
    -n openshift
    ```

2. Create the `view-secrets` role and bind it, along with the `view` cluster role, to the `default` service account used to run the quarkus application. These permissions allow the `default` service account to access secrets.
```zsh
oc create -f <(echo '
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  labels:
    app.kubernetes.io/name: camel-quarkus-rhoam-webhook-handler-api
    app.kubernetes.io/version: 1.0.0
    app.openshift.io/runtime: quarkus
  name: view-secrets
rules:
- apiGroups:
  - ""
  resources:
  - secrets
  verbs:
  - get
  - list
  - watch
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  labels:
    app.kubernetes.io/name: camel-quarkus-rhoam-webhook-handler-api
    app.kubernetes.io/version: 1.0.0
    app.openshift.io/runtime: quarkus
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
  labels:
    app.kubernetes.io/name: camel-quarkus-rhoam-webhook-handler-api
    app.kubernetes.io/version: 1.0.0
    app.openshift.io/runtime: quarkus
  name: default:view-secrets
roleRef:
  kind: Role
  apiGroup: rbac.authorization.k8s.io
  name: view-secrets
subjects:
- kind: ServiceAccount
  name: default
');
```

3. Create the `camel-quarkus-rhoam-webhook-handler-api` OpenShift application from the git repository
    ```zsh
    oc new-app https://github.com/jeannyil-apis-playground/apicurio-generated-projects.git \
    --context-dir=camel-quarkus-rhoam-webhook-handler-api \
    --name=camel-quarkus-rhoam-webhook-handler-api \
    --image-stream="openshift/openjdk-11-ubi8" \
    --labels=app.kubernetes.io/name=camel-quarkus-rhoam-webhook-handler-api \
    --labels=app.kubernetes.io/version=1.0.0 \
    --labels=app.openshift.io/runtime=quarkus
    ```

4. Follow the log of the S2I build
    ```zsh
    oc logs bc/camel-quarkus-rhoam-webhook-handler-api -f
    ```
    ```zsh
    Cloning "https://github.com/jeannyil-apis-playground/apicurio-generated-projects.git" ...
        Commit:	aaf528ba112cb014f57fc25041eb4f6acfac8c2f (Upgraded to Red Hat build of Quarkus 1.11)
        Author:	Jean Armand Nyilimbibi <jean.nyilimbibi@gmail.com>
        Date:	Thu May 27 00:03:10 2021 +0200
    [...]
    Successfully pushed image-registry.openshift-image-registry.svc:5000/camel-quarkus-jvm/camel-quarkus-rhoam-webhook-handler-api@sha256:509d233a0396f38fa00dd793abe52dd18c164eb3eda742696c269cff5d14e4d3
    Push successful
    ```

5. Create a non-secure route to expose the `camel-quarkus-rhoam-webhook-handler-api` service outside the OpenShift cluster
    ```zsh
    oc expose svc/camel-quarkus-rhoam-webhook-handler-api
    ```

## Testing the application on OpenShift

### Pre-requisites

- [**`curl`**](https://curl.se/) or [**`HTTPie`**](https://httpie.io/) command line tools. 
- [**`HTTPie`**](https://httpie.io/) has been used in the tests.

### Testing instructions:

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

            ```zsh
            echo 'PLAIN TEXT' | http -v POST $URL/webhook/amqpbridge content-type:application/xml
            ```
            ```zsh
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
    ```zsh
    http -v $URL/openapi.json
    ```
    ```zsh
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
    ```zsh
    http -v $URL/q/health
    ```
    ```zsh
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
    ```zsh
    http -v $URL/q/health/live
    ```
    ```zsh
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
    ```zsh
    http -v $URL/q/health/ready
    ```
    ```zsh
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
    ```zsh
    http -v $URL/q/metrics
    ```
    ```zsh
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
    ```zsh
    oc login ...
    ```

2. Create an OpenShift project or use your existing OpenShift project. For instance, to create `camel-quarkus-native`
    ```zsh
    oc new-project camel-quarkus-native --display-name="Apache Camel Quarkus Apps - Native Mode"
    ```
        
3. Create an `allInOne` Jaeger instance.
    1. **IF NOT ALREADY INSTALLED**:
        1. Install, via OLM, the `Red Hat OpenShift distributed tracing platform` (Jaeger) operator with an `AllNamespaces` scope. :warning: Needs `cluster-admin` privileges
            ```zsh
            oc create --save-config -f - <<EOF
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
            ```zsh
            watch oc get sub,csv
            ```
    2. Create the `allInOne` Jaeger instance.
        ```zsh
        oc create --save-config -f - <<EOF
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
    ```zsh
    oc create secret generic quarkus-amqpbroker-connection-secret \
    --from-literal=quarkus.qpid-jms.url="amqps://<AMQP_HOST_CHANGE_ME>:<AMQP_PORT_CHANGE_ME>?transport.trustAll=true&transport.verifyHost=false&amqp.idleTimeout=120000" \
    --from-literal=quarkus.qpid-jms.username=<CHANGE_ME> \
    --from-literal=quarkus.qpid-jms.password=<CHANGE_ME>
    ```

5. Create the `quarkus-opentracing-endpoint-secret` containing the _QUARKUS OPENTRACING_ [endpoint configuration options](https://quarkus.io/version/main/guides/opentracing#configuration-reference). These options are leveraged by the _Camel Quarkus Opentracing_ extension to connect to the jaeger collector. Adapt the `quarkus.jaeger.endpoint`according to your environment.

    :warning: _Replace values with your AMQP broker environment_
    ```zsh
    oc create secret generic quarkus-opentracing-endpoint-secret \
    --from-literal=quarkus.jaeger.endpoint="http://jaeger-all-in-one-inmemory-collector.camel-quarkus-native.svc:14268/api/traces"
    ``` 

6. Build a Linux executable using a container build. Compiling a Quarkus application to a native executable consumes a lot of memory during analysis and optimization. You can limit the amount of memory used during native compilation by setting the `quarkus.native.native-image-xmx` configuration property. Setting low memory limits might increase the build time.
    1. For Docker use:
        ```zsh
        ./mvnw package -Pnative -Dquarkus.native.container-build=true \
        -Dquarkus.native.native-image-xmx=7g
        ```
    2. For Podman use:
        ```zsh
        ./mvnw package -Pnative -Dquarkus.native.container-build=true \
        -Dquarkus.native.container-runtime=podman \
        -Dquarkus.native.native-image-xmx=7g
        ```
    ```zsh
    [...]
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildStep] Running Quarkus native-image plugin on GraalVM Version 20.3.3.0-Final (Mandrel Distribution) (Java Version 11.0.12+7)
    [INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildRunner] docker run --env LANG=C --rm -v /Users/jnyilimb/workdata/myGit/RedHatApiManagement/apicurio-generated-projects/camel-quarkus-rhoam-webhook-handler-api/target/camel-quarkus-rhoam-webhook-handler-api-1.0.0-native-image-source-jar:/project:z --name build-native-eWWyI quay.io/quarkus/ubi-quarkus-mandrel:20.3-java11 -J-Dsun.nio.ch.maxUpdateArraySize=100 -J-Djava.util.logging.manager=org.jboss.logmanager.LogManager -J-Dcom.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize=true -J-Dvertx.logger-delegate-factory-class-name=io.quarkus.vertx.core.runtime.VertxLogDelegateFactory -J-Dvertx.disableDnsResolver=true -J-Dio.netty.leakDetection.level=DISABLED -J-Dio.netty.allocator.maxOrder=3 -J-Duser.language=en -J-Duser.country=FR -J-Dfile.encoding=UTF-8 -H:InitialCollectionPolicy=com.oracle.svm.core.genscavenge.CollectionPolicy\$BySpaceAndTime -H:+JNI -H:+AllowFoldMethods -H:FallbackThreshold=0 -H:+ReportExceptionStackTraces -J-Xmx7g -H:+AddAllCharsets -H:EnableURLProtocols=http,https --enable-all-security-services -H:-UseServiceLoaderFeature -H:+StackTrace camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner -jar camel-quarkus-rhoam-webhook-handler-api-1.0.0-runner.jar
    [...]
    [INFO] [io.quarkus.deployment.QuarkusAugmentor] Quarkus augmentation completed in 1428418ms
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time:  24:03 min
    [INFO] Finished at: 2021-05-27T00:42:54+02:00
    [INFO] ------------------------------------------------------------------------
    ```

7. Create the `camel-quarkus-rhoam-webhook-handler-api` container image using the _OpenShift Docker build_ strategy. This strategy creates a container using a build configuration in the cluster.
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
        .....
        Uploading finished
        build.build.openshift.io/camel-quarkus-rhoam-webhook-handler-api-2 started
        Receiving source from STDIN as archive ...
        Replaced Dockerfile FROM image registry.access.redhat.com/ubi8/ubi-minimal:8.1
        Caching blobs under "/var/cache/blobs".
        [...]
        Pulling image registry.access.redhat.com/ubi8/ubi-minimal@sha256:4e6755fbb3af9502f60d7f0da12bad68217db3f92c2114f8867b76ac4e1d8bed ...
        [...]
        Successfully pushed image-registry.openshift-image-registry.svc:5000/camel-quarkus-native/camel-quarkus-rhoam-webhook-handler-api@sha256:9248d632e25d5b7dc8c5085b74fec3dba40fc4f1088c3d36f62c260ae4f47de9
        Push successful
        ```

8. Create the `view-secrets` role and bind it, along with the `view` cluster role, to the `default` service account used to run the quarkus application. These permissions allow the `default` service account to access secrets.
    ```zsh
    oc create -f <(echo '
    ---
    apiVersion: rbac.authorization.k8s.io/v1
    kind: Role
    metadata:
    labels:
        app.kubernetes.io/name: camel-quarkus-rhoam-webhook-handler-api
        app.kubernetes.io/version: 1.0.0
        app.openshift.io/runtime: quarkus
    name: view-secrets
    rules:
    - apiGroups:
    - ""
    resources:
    - secrets
    verbs:
    - get
    - list
    - watch
    ---
    apiVersion: rbac.authorization.k8s.io/v1
    kind: RoleBinding
    metadata:
    labels:
        app.kubernetes.io/name: camel-quarkus-rhoam-webhook-handler-api
        app.kubernetes.io/version: 1.0.0
        app.openshift.io/runtime: quarkus
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
    labels:
        app.kubernetes.io/name: camel-quarkus-rhoam-webhook-handler-api
        app.kubernetes.io/version: 1.0.0
        app.openshift.io/runtime: quarkus
    name: default:view-secrets
    roleRef:
    kind: Role
    apiGroup: rbac.authorization.k8s.io
    name: view-secrets
    subjects:
    - kind: ServiceAccount
    name: default
    ');
    ```

9. Deploy the `camel-quarkus-rhoam-webhook-handler-api` as a serverless application.
    ```zsh
    kn service create camel-quarkus-rhoam-webhook-handler-api \
    --label app.openshift.io/runtime=quarkus \
    --image image-registry.openshift-image-registry.svc:5000/camel-quarkus-native/camel-quarkus-rhoam-webhook-handler-api:latest
    ```

10. To verify that the `camel-quarkus-rhoam-webhook-handler-api` service is ready, enter the following command.
    ```zsh
    kn service list camel-quarkus-rhoam-webhook-handler-api
    ```
    The output in the column called "READY" reads `True` if the service is ready.

## Start-up time comparison on the same OpenShift cluster

### JVM mode

```zsh
2021-11-20 15:09:38,259 INFO  [org.apa.cam.qua.cor.CamelBootstrapRecorder] (main) Bootstrap runtime: org.apache.camel.quarkus.main.CamelMainRuntime
2021-11-20 15:09:38,370 INFO  [org.apa.cam.mai.BaseMainSupport] (main) Auto-configuration summary
2021-11-20 15:09:38,371 INFO  [org.apa.cam.mai.BaseMainSupport] (main)     camel.context.name=camel-quarkus-rhoam-webhook-handler-api
2021-11-20 15:09:39,268 INFO  [org.apa.cam.lan.xpa.XPathBuilder] (main) Created default XPathFactory org.apache.xpath.jaxp.XPathFactoryImpl@38fb50f8
2021-11-20 15:09:39,570 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) StreamCaching is enabled on CamelContext: camel-quarkus-rhoam-webhook-handler-api
2021-11-20 15:09:39,668 INFO  [org.apa.cam.imp.eng.DefaultStreamCachingStrategy] (main) StreamCaching in use with spool directory: /tmp/camel/camel-tmp-5F33B778E2D667B-0000000000000000 and rules: [Spool > 128K body size]
2021-11-20 15:09:39,760 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Routes startup summary (total:6 started:6)
2021-11-20 15:09:39,761 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started generate-error-response-route (direct://generateErrorResponse)
2021-11-20 15:09:39,761 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started ping-webhook-route (direct://pingWebhook)
2021-11-20 15:09:39,761 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started send-to-amqp-queue-route (direct://sendToAMQPQueue)
2021-11-20 15:09:39,761 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started get-openapi-spec-route (rest://get:/openapi.json)
2021-11-20 15:09:39,761 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started webhook-amqpbridge-ping-route (rest://get:/webhook/amqpbridge)
2021-11-20 15:09:39,761 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main)     Started webhook-amqpbridge-handler-route (rest://post:/webhook/amqpbridge)
2021-11-20 15:09:39,761 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.11.1 (camel-quarkus-rhoam-webhook-handler-api) started in 1s105ms (build:0ms init:914ms start:191ms)
2021-11-20 15:09:40,161 INFO  [io.quarkus] (main) camel-quarkus-rhoam-webhook-handler-api 1.0.0 on JVM (powered by Quarkus 2.2.3.Final-redhat-00013) started in 9.205s. Listening on: http://0.0.0.0:8080
2021-11-20 15:09:40,162 INFO  [io.quarkus] (main) Profile prod activated.
2021-11-20 15:09:40,162 INFO  [io.quarkus] (main) Installed features: [camel-amqp, camel-attachments, camel-bean, camel-core, camel-direct, camel-jackson, camel-jms, camel-microprofile-health, camel-microprofile-metrics, camel-openapi-java, camel-opentracing, camel-platform-http, camel-rest, camel-xml-jaxb, camel-xpath, cdi, config-yaml, jaeger, kubernetes, kubernetes-client, qpid-jms, smallrye-context-propagation, smallrye-health, smallrye-metrics, smallrye-opentracing, vertx, vertx-web]
```

### Native mode

```zsh
[...]
2021-05-26 23:04:03,107 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.7.0 (camel-1) is starting
2021-05-26 23:04:03,107 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) StreamCaching is enabled on CamelContext: camel-1
[...]
2021-05-26 23:04:03,108 INFO  [org.apa.cam.imp.eng.DefaultStreamCachingStrategy] (main) StreamCaching in use with spool directory: /tmp/camel/camel-tmp-8A2575A1F27C66F-0000000000000000 and rules: [Spool > 128K body size]
2021-05-26 23:04:03,109 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: ping-webhook-route started and consuming from: direct://pingWebhook
2021-05-26 23:04:03,109 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: generate-error-response-route started and consuming from: direct://generateErrorResponse
2021-05-26 23:04:03,109 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: send-to-amqp-queue-route started and consuming from: direct://sendToAMQPQueue
2021-05-26 23:04:03,109 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: get-openapi-spec-route started and consuming from: platform-http:///openapi.json
2021-05-26 23:04:03,109 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: webhook-amqpbridge-ping-route started and consuming from: platform-http:///webhook/amqpbridge
2021-05-26 23:04:03,109 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: webhook-amqpbridge-handler-route started and consuming from: platform-http:///webhook/amqpbridge
2021-05-26 23:04:03,109 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Total 6 routes, of which 6 are started
2021-05-26 23:04:03,109 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.7.0 (camel-1) started in 2ms
2021-05-26 23:04:03,111 INFO  [io.quarkus] (main) camel-quarkus-rhoam-webhook-handler-api 1.0.0 native (powered by Quarkus 1.11.6.Final-redhat-00001) started in 0.088s. Listening on: http://0.0.0.0:8080
2021-05-26 23:04:03,111 INFO  [io.quarkus] (main) Profile prod activated.
2021-05-26 23:04:03,111 INFO  [io.quarkus] (main) Installed features: [camel-amqp, camel-attachments, camel-bean, camel-core, camel-direct, camel-jackson, camel-jms, camel-microprofile-health, camel-microprofile-metrics, camel-openapi-java, camel-platform-http, camel-rest, camel-suport-xalan, camel-support-common, camel-support-commons-logging, camel-support-jackson-dataformat-xml, camel-support-spring, camel-xml-jaxb, camel-xpath, cdi, config-yaml, kubernetes, kubernetes-client, mutiny, qpid-jms, smallrye-context-propagation, smallrye-health, smallrye-metrics, vertx, vertx-web]
[...]
```
