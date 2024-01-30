package org.ivcode.mvn.util

import org.apache.maven.artifact.repository.metadata.Metadata
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.eclipse.jgit.attributes.Attributes
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.dircache.DirCacheBuilder
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.util.FS
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.time.Instant
import kotlin.io.path.*
import java.util.function.Function

public data class GitFileInfo (
    val id: ObjectId,
    val path: String,
    val attributes: Attributes,
    val fileMode: FileMode
)
public fun openRepository(file: File): Repository = RepositoryBuilder().run {
    val fileSystem = FS.DETECTED
    val key = RepositoryCache.FileKey.lenient(file, fileSystem)

    fs = fileSystem
    gitDir = key.file
    setMustExist(true)
    build().apply { close() }
}

public fun Repository.checkout(branch: String): RefUpdate.Result? {
    val ref = findRef(branch)
    val refUpdate = updateRef(Constants.HEAD, false)
    refUpdate.isForceUpdate = true

    return refUpdate.link(ref.name)
}

public fun Repository.getFileInfo(path: String): GitFileInfo? {
    val head = resolve("HEAD")
    val walk = RevWalk(this)
    val commit = walk.parseCommit(head.toObjectId())

    val treeWalk = TreeWalk(this)
    treeWalk.addTree(commit.tree)
    treeWalk.isRecursive = true
    treeWalk.filter = PathFilter.create(path)

    val info = mutableSetOf<GitFileInfo>()
    while (treeWalk.next()) {
        info.add(GitFileInfo(
            id = treeWalk.getObjectId(0),
            path = treeWalk.pathString,
            attributes = treeWalk.attributes,
            fileMode = treeWalk.fileMode,
        ))
    }

    if(info.size > 1) {
        throw IllegalArgumentException("found more than one object for the given path: $path")
    }

    return info.firstOrNull()
}

public fun Repository.readMetadata(groupId: String, artifactId: String): Metadata? {
    val path = getMetadataPath(groupId, artifactId).pathString
    val info = getFileInfo(path) ?: return null
    val loader = open(info.id)

    val metadata = ByteArrayInputStream(loader.bytes).use {
        MetadataXpp3Reader().read(it, true)
    }

    return metadata
}

public fun DirCacheBuilder.addDataEntry (
    inserter: ObjectInserter,
    path: String,
    data: ByteArray,
    fileMode: FileMode = FileMode.REGULAR_FILE,
    lastModified: Instant = Instant.now(),
): DirCacheEntry = DirCacheEntry(path).apply {
    this.fileMode = fileMode
    this.length = data.size
    this.setLastModified(lastModified)

    val id = inserter.insert(Constants.OBJ_BLOB, data)

    this.setObjectId(id)
    this@addDataEntry.add(this)
}

public fun DirCacheBuilder.addRawEntry (
    inserter: ObjectInserter,
    path: String,
    length: Long,
    input: InputStream,
    fileMode: FileMode = FileMode.REGULAR_FILE,
    lastModified: Instant = Instant.now(),
): DirCacheEntry = DirCacheEntry(path).apply {
    this.fileMode = fileMode
    this.length = length.toInt()
    this.setLastModified(lastModified)

    val id = inserter.insert(Constants.OBJ_BLOB, length, input)

    this.setObjectId(id)
    this@addRawEntry.add(this)
}

public fun <T> DirCache.use (func: Function<DirCache, T>): T {
    try {
        return func.apply(this)
    } finally {
        unlock()
    }
}

public fun <T>  DirCacheBuilder.use (func: Function<DirCacheBuilder, T>): T {
    try {
        return func.apply(this)
    } finally {
        finish()
    }
}

