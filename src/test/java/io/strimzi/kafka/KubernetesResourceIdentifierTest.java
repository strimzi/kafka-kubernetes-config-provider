/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka;

import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class KubernetesResourceIdentifierTest {
    @Test
    public void testValidResourceIdentifierParsing()    {
        KubernetesResourceIdentifier id = KubernetesResourceIdentifier.fromConfigString("my-namespace/my-resource");

        assertThat(id.getNamespace(), is("my-namespace"));
        assertThat(id.getName(), is("my-resource"));
    }

    @Test
    public void testInvalidResourceIdentifierParsing()    {
        ConfigException e = assertThrows(ConfigException.class, () -> KubernetesResourceIdentifier.fromConfigString("my-namespace"));
        assertThat(e.getMessage(), is("Invalid path my-namespace. It has to be in format <namespace>/<secret>."));

        e = assertThrows(ConfigException.class, () -> KubernetesResourceIdentifier.fromConfigString("my-namespace//my-resource"));
        assertThat(e.getMessage(), is("Invalid path my-namespace//my-resource. It has to be in format <namespace>/<secret>."));

        e = assertThrows(ConfigException.class, () -> KubernetesResourceIdentifier.fromConfigString("my-namespace/"));
        assertThat(e.getMessage(), is("Invalid path my-namespace/. It has to be in format <namespace>/<secret>."));

        e = assertThrows(ConfigException.class, () -> KubernetesResourceIdentifier.fromConfigString("my-namespace/my-resource/my-field"));
        assertThat(e.getMessage(), is("Invalid path my-namespace/my-resource/my-field. It has to be in format <namespace>/<secret>."));
    }

}
