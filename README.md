[![Build Status](https://dev.azure.com/cncf/strimzi/_apis/build/status/kafka-kubernetes-config-provider?branchName=main)](https://dev.azure.com/cncf/strimzi/_build/latest?definitionId=32&branchName=main)
[![GitHub release](https://img.shields.io/github/release/strimzi/kafka-kubernetes-config-provider.svg)](https://github.com/strimzi/kafka-kubernetes-config-provider/releases/latest)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.strimzi/kafka-kubernetes-config-provider/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.strimzi/kafka-kubernetes-config-provider)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Twitter Follow](https://img.shields.io/twitter/follow/strimziio.svg?style=social&label=Follow&style=for-the-badge)](https://twitter.com/strimziio)

# Kubernetes Configuration Provider for Apache Kafka

Apache Kafka supports pluggable configuration providers which can load configuration data from external sources.
The configuration providers in this repo can be used to load data from Kubernetes Secrets and Config Maps.
It can be used in all Kafka components and does not depend on the other Strimzi components. 
So you could, for example, use it with your producer or consumer applications even if you don't use the Strimzi operators to provide your Kafka cluster.
One of the example use-cases is to load certificates or JAAS configuration from Kubernetes Secrets.

## Adding the Kubernetes Configuration Provider to Apache Kafka clients

You can add Kubernetes Configuration Provider as any other Java dependency using Maven or any other build tool.
For example:

```xml
<dependency>
    <groupId>io.strimzi</groupId>
    <artifactId>kafka-kubernetes-config-provider</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Adding the Kubernetes Configuration Provider to Apache Kafka server components

To add Kubernetes Configuration Provider to the Apache Kafka server distribution, you can download the ZIP file and unpack it into Kafka's `libs` directory.

## Using the configuration provider

First, you need to initializer the configuration provider.
For use with Kubernetes Secrets, use:

```properties
config.providers=secrets
config.providers.secrets.class=io.strimzi.kafka.KubernetesSecretConfigProvider
```

For use with Kubernetes Config Maps, use:

```properties
config.providers=configmaps
config.providers.configmaps.class=io.strimzi.kafka.KubernetesConfigMapConfigProvider
```

You can also use both together:

```properties
config.providers=secrets,configmaps
config.providers.secrets.class=io.strimzi.kafka.KubernetesSecretConfigProvider
config.providers.configmaps.class=io.strimzi.kafka.KubernetesConfigMapConfigProvider
```

Once you initialize it, you can use it to load date from Kubernetes.
For example to load TLS certificates for a Kafka cluster running on Kubernetes (use [Strimzi](https://strimzi.io) for it 😉 ): 

```properties
security.protocol=SSL
ssl.keystore.type=PEM
ssl.keystore.certificate.chain=${secrets:myproject/my-user:user.crt}
ssl.keystore.key=${secrets:myproject/my-user:user.key}
ssl.truststore.type=PEM
ssl.truststore.certificates=${secrets:myproject/my-cluster-cluster-ca-cert:ca.crt}
```

The config provider configuration is in the form `<NAMESPACE>/<RESOURCE-NAME>:<KEY>`.
Where:
* The `<NAMESPACE>` is the namespace where the Secret or Config Map exists
* The `<RESOURCE-NAME>` is the name of the Secret or Config Map
* The `<KEY>` is the key under which the configuration value is stored in the Secret or Config Map

## Configuring the Kubernetes client

The Kubernetes Config Provider is using the [Fabric8 Kubernetes Client](https://github.com/fabric8io/kubernetes-client).
You can configure it using Java system properties, environment variables, Kube config files or ServiceAccount tokens.
All available configuration options are described in the [client documentation](https://github.com/fabric8io/kubernetes-client#configuring-the-client).
By default, it will try to automatically find the available configuration - for example from the Kube config file (`~/.kube/config`) or from the ServiceAccount if running inside Kubernetes Pod.

### RBAC rights

The Kubernetes account used by the Kubernetes Configuration Provider needs to have access to the Config Maps or Secrets.
The only RBAC rights it needs is the `get` access rights on a given resource.
For example:

```yaml
- apiGroups: [""]
  resources: ["secrets"]
  resourceNames: ["my-user", "my-cluster-cluster-ca-cert"]
  verbs: ["get"]
```

It does not need any other access rights.
