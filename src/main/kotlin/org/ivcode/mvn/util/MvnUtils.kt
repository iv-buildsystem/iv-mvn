package org.ivcode.mvn.util

import org.apache.maven.artifact.repository.metadata.Metadata
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path

private const val VERSION_SNAPSHOT_SUFFIX = "-SNAPSHOT"
private const val METADATA_LAST_UPDATED_PATTERN = "yyyyMMddHHmmss"

private const val METADATA_FILENAME = "maven-metadata.xml"
private val METADATA_LAST_UPDATED_FORMATTER = DateTimeFormatter.ofPattern(METADATA_LAST_UPDATED_PATTERN)

public fun createMetadata(groupId: String, artifactId: String): Metadata = Metadata().apply {
    setGroupId(groupId)
    setArtifactId(artifactId)
}

public fun isSnapshot(version: String): Boolean =
    version.trim().endsWith(VERSION_SNAPSHOT_SUFFIX)

public fun toPath(groupId: String, artifactId: String, version: String? = null): Path {
    return if(version!=null) {
        Path("${groupId.replace('.','/')}/${artifactId}/${version}")
    } else {
        Path("${groupId.replace('.','/')}/${artifactId}")
    }
}

public fun getMetadataPath(groupId: String, artifactId: String): Path {
    return toPath(groupId, artifactId).resolve(METADATA_FILENAME)
}

public fun Metadata.toPath(): Path = toPath(
    groupId = this.groupId,
    artifactId = this.artifactId,
    version = this.version
)

public fun Metadata.lastModifiedData(): Instant? {
    val lastUpdated = versioning.lastUpdated ?: return null
    val ldt = LocalDateTime.parse(lastUpdated, METADATA_LAST_UPDATED_FORMATTER)

    return ldt.atZone(ZoneId.systemDefault()).toInstant()
}

public fun Metadata.isVersioned(version: String): Boolean =
    // There's conflicting information about case-sensitivity in versions.
    // From what I can tell, they are case-sensitive through and through
    !versioning.versions.none { it == version }


/**
 * Adds a version to the current metadata if the version does not already exist. If the version already
 * exists, there is no change.
 *
 * Updated Fields (if version not already defined):
 *  - version - adds the version
 *  - latest - sets the given version to the latest
 *  - release - if not a snapshot, the release is updated to the given version
 *  - lastUpdated - updated to the given time
 *
 *  @return a new instance of the metadata. `null` is returned if not updated
 */
public fun Metadata.addVersion(version: String, lastUpdated: Instant): Metadata? {
    if(this.version!=null) {
        // If the artifact version is defined, this metadata represents snapshot information
        throw IllegalArgumentException("snapshot metadata not supported")
    }

    if(isVersioned(version)) {
        return null
    }

    val new = this.clone()

    new.versioning.versions.add(version)
    new.versioning.latest = version
    if(!isSnapshot(version)) {
        new.versioning.release = version
    }
    new.versioning.lastUpdated = METADATA_LAST_UPDATED_FORMATTER.format(lastUpdated)

    return new
}
