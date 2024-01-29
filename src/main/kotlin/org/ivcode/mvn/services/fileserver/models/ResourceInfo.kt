package org.ivcode.mvn.services.fileserver.models

import org.springframework.http.MediaType
import java.net.URI
import java.nio.file.Path
import java.time.Instant

public data class ResourceInfo (
    /** url path */
    val uri: URI,
    /** path relative to the repository */
    val path: Path,
    val name: String,
    val mimeType: MediaType? = null,
    val isDirectory: Boolean,
    val isRoot: Boolean,
    val lastModified: Instant,
    val size: Long,
    val children: List<ResourceChildInfo>? = null
)

public data class ResourceChildInfo (
    val path: Path,
    val name: String,
    val isDirectory: Boolean,
    val lastModified: Instant,
    val size: Long,
)