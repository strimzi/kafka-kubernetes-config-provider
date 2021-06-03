/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Apache Kafka configuration provider to load configuration from Kubernetes Secrets
 */
public final class KubernetesSecretConfigProvider extends AbstractKubernetesConfigProvider<Secret, SecretList, Resource<Secret>> {
    public KubernetesSecretConfigProvider() {
        super("Secret");
    }

    protected MixedOperation<Secret, SecretList, Resource<Secret>> operator()    {
        return client.secrets();
    }

    @Override
    Map<String, String> valuesFromResource(Secret resource) {
        Map<String, String> encodedValues = resource.getData();
        Map<String, String> values = new HashMap<>(encodedValues.size());

        for (Map.Entry<String, String> entry : encodedValues.entrySet())   {
            values.put(entry.getKey(), new String(Base64.getDecoder().decode(entry.getValue()), StandardCharsets.UTF_8));
        }

        return values;
    }
}
