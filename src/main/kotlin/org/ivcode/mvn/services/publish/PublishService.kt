package org.ivcode.mvn.services.publish

public interface PublishService {

    /**
     * Publishes maven artifacts.
     * Note: If a snapshot version is specified, this method will upload all versions within the snapshot
     */
    public fun publish(group: String, artifactId: String, version: String)
}