/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka;

import io.fabric8.kubernetes.client.Client;
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
     * Parses the path to the Kubernetes resource in the NAMESPACE/RESOURCE-NAME format or RESOURCE-NAME for use with
     * the default namespace. Throws Kafka ConfigException if it fails to parse the resource identifier.
     *
     * @param client    Instance of the Kubernetes Client used to get the default namespace if needed
     * @param path      The Kubernetes resource path
     *
     * @return          Instance of the KubernetesResourceIdentifier class
     */
    public static KubernetesResourceIdentifier fromConfigString(Client client, String path)    {
        if (!path.matches("([a-z0-9.-]+/)?[a-z0-9.-]+")) {
            throw new ConfigException("Invalid path " + path + ". It has to be in format <namespace>/<secret> (or <secret> for default namespace).");
        }

        String[] pathSegments = path.split("/");

        if (pathSegments.length == 1)   {
            return new KubernetesResourceIdentifier(client.getNamespace(), pathSegments[0]);
        } else if (pathSegments.length == 2)    {
            return new KubernetesResourceIdentifier(pathSegments[0], pathSegments[1]);
        } else  {
            // Should never happen really => the regex should capture all invalid
            // But it handles the missing return error
            throw new ConfigException("Invalid path " + path + ". It has to be in format <namespace>/<secret> (or <secret> for default namespace).");
        }
    }

    /**
     * Returns the namespace of the resource
     *
     * @return  Namespace of the resource
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the name of the resource
     *
     * @return  Name of the resource
     */
    public String getName() {
        return name;
    }
}
