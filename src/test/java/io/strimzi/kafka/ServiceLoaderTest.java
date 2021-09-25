/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka;

import org.apache.kafka.common.config.provider.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceLoaderTest {
    @Test
    public void testServiceLoaderDiscovery() {
        ServiceLoader<ConfigProvider> serviceLoader = ServiceLoader.load(ConfigProvider.class);

        boolean configMapProviderDiscovered = false;
        boolean secretProviderDiscovered = false;

        for (ConfigProvider service : serviceLoader)    {
            System.out.println(service.getClass());
            if (service instanceof KubernetesSecretConfigProvider) {
                secretProviderDiscovered = true;
            } else if (service instanceof KubernetesConfigMapConfigProvider) {
                configMapProviderDiscovered = true;
            }
        }

        assertTrue(secretProviderDiscovered);
        assertTrue(configMapProviderDiscovered);
    }
}
