package org.ivcode.mvn.util

import org.apache.maven.artifact.repository.metadata.Metadata
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer
import org.eclipse.jgit.attributes.Attributes
import org.eclipse.jgit.dircache.DirCacheBuilder
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.util.FS
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.*

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
    updateRef(Constants.HEAD, ref==null)
    val refUpdate = updateRef(Constants.HEAD, ref == null)
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
    val path = toPath(groupId, artifactId).pathString
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
) {
    val entry = DirCacheEntry(path)

    entry.fileMode = fileMode
    entry.length = data.size
    entry.setLastModified(lastModified)

    val id = inserter.insert(Constants.OBJ_BLOB, data)

    entry.setObjectId(id)
    add(entry)
}

public fun DirCacheBuilder.addRawEntry (
    inserter: ObjectInserter,
    path: String,
    length: Long,
    input: InputStream,
    fileMode: FileMode = FileMode.REGULAR_FILE,
    lastModified: Instant = Instant.now(),
) {
    val entry = DirCacheEntry(path)

    entry.fileMode = fileMode
    entry.length = length.toInt()
    entry.setLastModified(lastModified)

    val id = inserter.insert(Constants.OBJ_BLOB, length, input)

    entry.setObjectId(id)
    add(entry)
}

