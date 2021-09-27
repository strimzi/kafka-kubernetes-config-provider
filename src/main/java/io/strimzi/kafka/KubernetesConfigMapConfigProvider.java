/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.util.Map;

/**
 * Apache Kafka configuration provider to load configuration from Kubernetes Config Maps
 */
public final class KubernetesConfigMapConfigProvider extends AbstractKubernetesConfigProvider<ConfigMap, ConfigMapList, Resource<ConfigMap>> {
    public KubernetesConfigMapConfigProvider() {
        super("ConfigMap");
    }

    @Override
    protected MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> operator()    {
        return client.configMaps();
    }

    @Override
    protected Map<String, String> valuesFromResource(ConfigMap resource) {
        return resource.getData();
    }
}
