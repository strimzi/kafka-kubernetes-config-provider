/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka;

import org.apache.kafka.common.config.ConfigException;

/**
 * Used to represent a namespaced Kubernetes resource by its namespace and name
 */
final class KubernetesResourceIdentifier {
    private final String namespace;
    private final String name;

    private KubernetesResourceIdentifier(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    /**
     * Parses the path to the Kubernetes resource in the NAMESPACE/RESOURCE-NAME format. Throws Kafka ConfigException if
     * it fails to parse the resource identifier.
     *
     * @param path  The Kubernetes resource path
     *
     * @return      Instance of the KubernetesResourceIdentifier class
     */
    public static KubernetesResourceIdentifier fromConfigString(String path)    {
        String[] pathSegments = path.split("/");

        if (pathSegments.length != 2)   {
            throw new ConfigException("Invalid path " + path + ". It has to be in format <namespace>/<secret>.");
        }

        return new KubernetesResourceIdentifier(pathSegments[0], pathSegments[1]);
    }

    /**
     * Returns the namespace of the resource
     *
     * @return
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the name of the resource
     *
     * @return
     */
    public String getName() {
        return name;
    }
}
