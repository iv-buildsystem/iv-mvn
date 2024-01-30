package org.ivcode.mvn.util

import org.apache.maven.artifact.repository.metadata.Metadata
import org.apache.maven.artifact.repository.metadata.Versioning
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path

private const val VERSION_SNAPSHOT_SUFFIX = "-SNAPSHOT"
private const val METADATA_LAST_UPDATED_PATTERN = "uuuuMMddHHmmss"

private const val METADATA_FILENAME = "maven-metadata.xml"
private val METADATA_LAST_UPDATED_FORMATTER = DateTimeFormatter.ofPattern(METADATA_LAST_UPDATED_PATTERN)

public enum class MvnHashType(
    public val extension: String
) {
    MD5("md5") {
        override fun hex(data: ByteArray): String = data.md5Hex()
    },
    SHA1("sha1") {
        override fun hex(data: ByteArray): String = data.sha1Hex()
    },
    SHA256("sha256") {
        override fun hex(data: ByteArray): String = data.sha256Hex()
    },
    SHA512("sha512") {
        override fun hex(data: ByteArray): String = data.sha512Hex()
    };

    public abstract fun hex(data: ByteArray): String
}

public fun createMetadata(groupId: String, artifactId: String): Metadata = Metadata().apply {
    setGroupId(groupId)
    setArtifactId(artifactId)
    versioning = Versioning()
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

public fun getMetadataPath(
    groupId: String,
    artifactId: String,
    version: String? = null,
    hashType: MvnHashType? = null
): Path =
    toPath(groupId, artifactId, version).resolve("$METADATA_FILENAME${if(hashType!=null) {".${hashType.extension}"} else {""} }")

public fun Metadata.toPath(hashType: MvnHashType? = null): Path = getMetadataPath (
    groupId = this.groupId!!,
    artifactId = this.artifactId!!,
    version = this.version,
    hashType = hashType
)

public fun Metadata.lastModifiedData(): Instant? {
    val lastUpdated = versioning.lastUpdated ?: return null
    val ldt = LocalDateTime.parse(lastUpdated, METADATA_LAST_UPDATED_FORMATTER)

    return ldt.atZone(ZoneId.systemDefault()).toInstant()
}

public fun Metadata.isVersioned(version: String): Boolean =
    // There's conflicting information about case-sensitivity in versions.
    // From what I can tell, they are case-sensitive through and through
    !(versioning?.versions?.none { it == version } ?: true)


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
    new.versioning.lastUpdated = METADATA_LAST_UPDATED_FORMATTER.withZone(ZoneId.systemDefault()).format(lastUpdated)

    return new
}
