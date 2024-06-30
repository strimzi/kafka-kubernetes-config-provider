/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.apache.kafka.common.config.ConfigData;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.provider.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract class for Kafka configuration providers using Kubernetes resources
 *
 * @param <T>   Resource
 * @param <L>   Resource list
 * @param <R>   Kubernetes resource
 */
abstract class AbstractKubernetesConfigProvider<T extends HasMetadata, L extends KubernetesResourceList<T>, R extends Resource<T>> implements ConfigProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractKubernetesConfigProvider.class);
    private static final String SEPARATOR_CONFIG_NAME = "separator";

    protected final String kind;

    protected KubernetesClient client;
    private String separator = System.lineSeparator();

    /**
     * Creates the configuration provider
     *
     * @param kind  Kind of the Kubernetes resource handled by the provider implementation
     */
    AbstractKubernetesConfigProvider(String kind) {
        this.kind = kind;
    }

    // Abstract methods
    protected abstract MixedOperation<T, L, R> operator();

    protected abstract Map<String, String> valuesFromResource(T resource);

    // Methods from Kafka ConfigProvider
    @Override
    public void close() throws IOException {
        LOG.info("Closing Kubernetes {} config provider", kind);
        client.close();
    }

    @Override
    public void configure(Map<String, ?> config) {
        LOG.info("Configuring Kubernetes {} config provider with configuration {}", kind, config);

        if (config.get(SEPARATOR_CONFIG_NAME) != null) {
            separator = (String) config.get(SEPARATOR_CONFIG_NAME);
        }

        client = new KubernetesClientBuilder().build();
    }

    @Override
    public ConfigData get(String path) {
        return getValues(path, null);
    }

    @Override
    public ConfigData get(String path, Set<String> keys) {
        return getValues(path, keys);
    }

    /**
     * Gets the values from the Kubernetes resource.
     *
     * @param path  Path to the Kubernetes resource
     * @param keys  Keys which should be extracted from the resource
     *
     * @return      Kafka ConfigData with the configuration
     */
    private ConfigData getValues(String path, Set<String> keys)    {
        Map<String, String> values = valuesFromResource(getResource(path));
        Map<String, String> configs = new HashMap<>(0);

        if (keys == null)   {
            configs.putAll(values);
        } else {
            for (String key : keys) {
                PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + key);
                configs.put(key, values.entrySet().stream().filter(entry -> pathMatcher.matches(Paths.get(entry.getKey()))).sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).collect(Collectors.joining(separator)));
            }
        }

        return new ConfigData(configs);
    }

    // Kubernetes helper methods

    /**
     * Gets the resource from Kubernetes
     *
     * @param path  Path to the Kubernetes resource
     *
     * @return      Resource retrieved from the Kubernetes cluster
     */
    protected T getResource(String path)   {
        final KubernetesResourceIdentifier resourceIdentifier = KubernetesResourceIdentifier.fromConfigString(client, path);

        LOG.info("Retrieving configuration from {} {} in namespace {}", kind, resourceIdentifier.getName(), resourceIdentifier.getNamespace());

        try {
            T resource = operator().inNamespace(resourceIdentifier.getNamespace()).withName(resourceIdentifier.getName()).get();

            if (resource == null)   {
                throw new ConfigException(kind +  " " + resourceIdentifier.getName() + " in namespace " + resourceIdentifier.getNamespace() + " not found");
            }

            return resource;
        } catch (KubernetesClientException e)   {
            LOG.error("Failed to retrieve " + kind +  " " + resourceIdentifier.getName() + " from Kubernetes namespace " + resourceIdentifier.getNamespace(), e);
            throw new ConfigException("Failed to retrieve " + kind +  " " + resourceIdentifier.getName() + " from Kubernetes namespace " + resourceIdentifier.getNamespace());
        }
    }
}
