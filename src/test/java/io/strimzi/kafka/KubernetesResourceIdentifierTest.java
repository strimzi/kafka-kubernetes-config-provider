/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka;

import io.fabric8.kubernetes.client.Client;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KubernetesResourceIdentifierTest {
    private static Client client;

    @BeforeAll
    static void setUp()   {
        client = mock(Client.class);
        when(client.getNamespace()).thenReturn("default-namespace");
    }

    @Test
    public void testValidResourceIdentifierParsing()    {
        KubernetesResourceIdentifier id = KubernetesResourceIdentifier.fromConfigString(client, "my-namespace/my-resource");

        assertThat(id.getNamespace(), is("my-namespace"));
        assertThat(id.getName(), is("my-resource"));
    }

    @Test
    public void testValidResourceIdentifierParsingWithoutNamespace()    {
        KubernetesResourceIdentifier id = KubernetesResourceIdentifier.fromConfigString(client, "my-resource");

        assertThat(id.getNamespace(), is("default-namespace"));
        assertThat(id.getName(), is("my-resource"));
    }

    @Test
    public void testInvalidResourceIdentifierParsing()    {
        Exception e = assertThrows(ConfigException.class, () -> KubernetesResourceIdentifier.fromConfigString(client, "my-namespace//my-resource"));
        assertThat(e.getMessage(), is("Invalid path my-namespace//my-resource. It has to be in format <namespace>/<secret> (or <secret> for default namespace)."));

        e = assertThrows(ConfigException.class, () -> KubernetesResourceIdentifier.fromConfigString(client, "my-namespace/"));
        assertThat(e.getMessage(), is("Invalid path my-namespace/. It has to be in format <namespace>/<secret> (or <secret> for default namespace)."));

        e = assertThrows(ConfigException.class, () -> KubernetesResourceIdentifier.fromConfigString(client, "/my-namespace"));
        assertThat(e.getMessage(), is("Invalid path /my-namespace. It has to be in format <namespace>/<secret> (or <secret> for default namespace)."));

        e = assertThrows(ConfigException.class, () -> KubernetesResourceIdentifier.fromConfigString(client, "my-namespace/my-resource/my-field"));
        assertThat(e.getMessage(), is("Invalid path my-namespace/my-resource/my-field. It has to be in format <namespace>/<secret> (or <secret> for default namespace)."));

        e = assertThrows(ConfigException.class, () -> KubernetesResourceIdentifier.fromConfigString(client, "my-namespace/my-resource/"));
        assertThat(e.getMessage(), is("Invalid path my-namespace/my-resource/. It has to be in format <namespace>/<secret> (or <secret> for default namespace)."));

        e = assertThrows(ConfigException.class, () -> KubernetesResourceIdentifier.fromConfigString(client, "/my-namespace/my-resource"));
        assertThat(e.getMessage(), is("Invalid path /my-namespace/my-resource. It has to be in format <namespace>/<secret> (or <secret> for default namespace)."));
    }
}
