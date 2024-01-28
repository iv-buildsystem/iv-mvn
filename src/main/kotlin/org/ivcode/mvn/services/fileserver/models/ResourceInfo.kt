package org.ivcode.mvn.services.fileserver.models

import org.springframework.http.MediaType
import java.net.URI
import java.nio.file.Path

public data class ResourceInfo (
    /** url path */
    val uri: URI,
    /** path relative to the repository */
    val path: String,
    val name: String,
    val mimeType: MediaType? = null,
    val isDirectory: Boolean,
    val isRoot: Boolean,
    val children: List<ResourceChildInfo>? = null
)

public data class ResourceChildInfo (
    val name: String,
    val isDirectory: Boolean
)