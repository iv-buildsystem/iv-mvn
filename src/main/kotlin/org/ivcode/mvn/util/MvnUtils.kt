package org.ivcode.mvn.util

import org.apache.maven.artifact.repository.metadata.Metadata
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path

private const val METADATA_LAST_UPDATED_PATTERN = "yyyyMMddHHmmss"

public fun toPath(groupId: String, artifactId: String, version: String? = null): Path {
    return if(version!=null) {
        Path("${groupId.replace('.','/')}/${artifactId}/${version}")
    } else {
        Path("${groupId.replace('.','/')}/${artifactId}")
    }
}

public fun Metadata.toPath(): Path = toPath(
    groupId = this.groupId,
    artifactId = this.artifactId,
    version = this.version
)

public fun Metadata.lastModifiedData(): Instant? {
    val lastUpdated = versioning.lastUpdated ?: return null
    val p = DateTimeFormatter.ofPattern(METADATA_LAST_UPDATED_PATTERN)
    val ldt = LocalDateTime.parse(lastUpdated, p)

    return ldt.atZone(ZoneId.systemDefault()).toInstant()
}