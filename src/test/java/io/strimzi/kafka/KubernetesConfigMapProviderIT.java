/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.apache.kafka.common.config.ConfigData;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class KubernetesConfigMapProviderIT {
    private static final String RESOURCE_NAME = "my-test-config-map";

    private static String namespace;
    private static KubernetesClient client;
    private static KubernetesConfigMapConfigProvider provider;

    @BeforeAll
    public static void beforeAll()   {
        provider = new KubernetesConfigMapConfigProvider();
        provider.configure(emptyMap());

        client = new KubernetesClientBuilder().build();
        namespace = client.getNamespace();

        ConfigMap cm = new ConfigMapBuilder()
                .withNewMetadata()
                    .withName(RESOURCE_NAME)
                    .withNamespace(namespace)
                .endMetadata()
                .addToData("test-key-1", "test-value-1")
                .addToData("test-key-2", "test-value-2")
                .addToData("test-key-3", "test-value-3")
                .build();

        client.configMaps().resource(cm).create();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        client.configMaps().inNamespace(namespace).withName(RESOURCE_NAME).delete();
        provider.close();
    }

    @Test
    public void testAllValues() {
        ConfigData config = provider.get(namespace + "/" + RESOURCE_NAME);
        Map<String, String> data = config.data();

        assertThat(data.size(), is(3));
        assertThat(data.get("test-key-1"), is("test-value-1"));
        assertThat(data.get("test-key-2"), is("test-value-2"));
        assertThat(data.get("test-key-3"), is("test-value-3"));
    }

    @Test
    public void testSomeValues() {
        ConfigData config = provider.get(namespace + "/" + RESOURCE_NAME, new HashSet<>(Arrays.asList("test-key-1", "test-key-3")));
        Map<String, String> data = config.data();

        assertThat(data.size(), is(2));
        assertThat(data.get("test-key-1"), is("test-value-1"));
        assertThat(data.get("test-key-3"), is("test-value-3"));
    }

    @Test
    public void testOneValue() {
        ConfigData config = provider.get(namespace + "/" + RESOURCE_NAME, Collections.singleton("test-key-2"));
        Map<String, String> data = config.data();

        assertThat(data.size(), is(1));
        assertThat(data.get("test-key-2"), is("test-value-2"));
    }

    @Test
    public void testDefaultNamespace() {
        ConfigData config = provider.get(RESOURCE_NAME);
        Map<String, String> data = config.data();

        assertThat(data.size(), is(3));
        assertThat(data.get("test-key-1"), is("test-value-1"));
        assertThat(data.get("test-key-2"), is("test-value-2"));
        assertThat(data.get("test-key-3"), is("test-value-3"));
    }

    @Test
    public void testNonExistentConfigMap() {
        assertThrows(ConfigException.class, () -> provider.get(namespace + "/i-do-not-exist"));
        assertThrows(ConfigException.class, () -> provider.get("i-do-not-exist/i-do-not-exist-either"));
        assertThrows(ConfigException.class, () -> provider.get("i-do-not-exist"));
    }
}
