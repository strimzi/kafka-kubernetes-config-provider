/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.apache.kafka.common.config.ConfigData;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class KubernetesSecretProviderIT {
    private static final String RESOURCE_NAME = "my-test-secret";

    private static String namespace;
    private static KubernetesClient client;
    private static KubernetesSecretConfigProvider provider;

    @BeforeAll
    public static void beforeAll()   {
        provider = new KubernetesSecretConfigProvider();
        provider.configure(emptyMap());

        client = new KubernetesClientBuilder().build();
        namespace = client.getNamespace();

        Secret secret = new SecretBuilder()
                .withNewMetadata()
                    .withName(RESOURCE_NAME)
                    .withNamespace(namespace)
                .endMetadata()
                .addToData("truststore.p12", Base64.getEncoder().encodeToString("pkcs12-truststore".getBytes()))
                .addToData("ca.crt", Base64.getEncoder().encodeToString("first-ca".getBytes()))
                .addToData("ca2.crt", Base64.getEncoder().encodeToString("second-ca".getBytes()))
                .addToStringData("ca3.crt", "third-ca")
                .build();

        client.secrets().resource(secret).create();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        client.secrets().inNamespace(namespace).withName(RESOURCE_NAME).delete();
        provider.close();
    }

    @Test
    public void testAllValues() {
        ConfigData config = provider.get(namespace + "/" + RESOURCE_NAME);
        Map<String, String> data = config.data();

        assertThat(data.size(), is(4));
        assertThat(data.get("ca.crt"), is("first-ca"));
        assertThat(data.get("ca2.crt"), is("second-ca"));
        assertThat(data.get("ca3.crt"), is("third-ca"));
        assertThat(data.get("truststore.p12"), is("pkcs12-truststore"));
    }

    @Test
    public void testSomeValues() {
        ConfigData config = provider.get(namespace + "/" + RESOURCE_NAME, new HashSet<>(Arrays.asList("ca.crt", "*.crt", "truststore.p12")));
        Map<String, String> data = config.data();

        assertThat(data.size(), is(3));
        assertThat(data.get("ca.crt"), is("first-ca"));
        assertThat(data.get("*.crt"), is("first-ca" + System.lineSeparator() + "second-ca" + System.lineSeparator() + "third-ca"));
        assertThat(data.get("truststore.p12"), is("pkcs12-truststore"));
    }

    @Test
    public void testOneValue() {
        ConfigData config = provider.get(namespace + "/" + RESOURCE_NAME, Collections.singleton("ca2.crt"));
        Map<String, String> data = config.data();

        assertThat(data.size(), is(1));
        assertThat(data.get("ca2.crt"), is("second-ca"));
    }

    @Test
    public void testPatternValue() {
        ConfigData config = provider.get(namespace + "/" + RESOURCE_NAME, Collections.singleton("*.crt"));
        Map<String, String> data = config.data();

        assertThat(data.size(), is(1));
        assertThat(data.get("*.crt"), is("first-ca" + System.lineSeparator() + "second-ca" + System.lineSeparator() + "third-ca"));
    }

    @Test
    public void testNotMatchingPatternValue() {
        ConfigData config = provider.get(namespace + "/" + RESOURCE_NAME, Collections.singleton("*.cert"));
        Map<String, String> data = config.data();

        assertThat(data.size(), is(1));
        assertThat(data.get("*.cert"), is(""));
    }

    @Test
    public void testDefaultNamespace() {
        ConfigData config = provider.get(RESOURCE_NAME);
        Map<String, String> data = config.data();

        assertThat(data.size(), is(4));
        assertThat(data.get("ca.crt"), is("first-ca"));
        assertThat(data.get("ca2.crt"), is("second-ca"));
        assertThat(data.get("ca3.crt"), is("third-ca"));
        assertThat(data.get("truststore.p12"), is("pkcs12-truststore"));
    }

    @Test
    public void testNonExistentSecret() {
        assertThrows(ConfigException.class, () -> provider.get(namespace + "/i-do-not-exist"));
        assertThrows(ConfigException.class, () -> provider.get("i-do-not-exist/i-do-not-exist-either"));
        assertThrows(ConfigException.class, () -> provider.get("i-do-not-exist"));
    }

    @Test
    public void testCustomSeparator() {
        KubernetesSecretConfigProvider customProvider = new KubernetesSecretConfigProvider();
        customProvider.configure(Map.of("separator", ","));

        ConfigData config = customProvider.get(namespace + "/" + RESOURCE_NAME, Collections.singleton("*.crt"));
        Map<String, String> data = config.data();

        assertThat(data.size(), is(1));
        assertThat(data.get("*.crt"), is("first-ca,second-ca,third-ca"));
    }
}
