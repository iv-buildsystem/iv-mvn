package org.ivcode.mvn.services.fileserver.filesystem

import org.ivcode.mvn.exceptions.ConflictException
import org.ivcode.mvn.exceptions.ForbiddenException
import org.ivcode.mvn.exceptions.NotFoundException
import org.ivcode.mvn.services.fileserver.FileServerService
import org.ivcode.mvn.services.fileserver.models.ResourceChildInfo
import org.ivcode.mvn.services.fileserver.models.ResourceInfo
import org.ivcode.mvn.util.*
import org.ivcode.mvn.util.deleteRecursively
import org.springframework.http.MediaType
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.file.*
import java.util.function.Function
import kotlin.io.path.*

/**
 * File-System based maven repository
 */
public class FileServerServiceFileSystemImpl (
    private val repositoryName: String,
    mvnRoot: Path
) : FileServerService {
    private val root: Path = mvnRoot.full()

    init {
        if(!root.exists()) {
            root.createDirectories()
        }
    }

    override fun getPathInfo(path: Path): ResourceInfo {
        val resolvedPath = root.resolve(path).full()
        checkFile(resolvedPath)

        if(!resolvedPath.exists()) {
            throw NotFoundException()
        }

        return ResourceInfo(
            uri = URI.create("/${repositoryName}/${resolvedPath.relativeTo(root)}"),
            path = path,
            name = resolvedPath.name,
            mimeType = getMime(resolvedPath),
            isDirectory = resolvedPath.isDirectory(),
            isRoot = resolvedPath.isSameFileAs(root),
            lastModified = resolvedPath.getLastModifiedTime().toInstant(),
            size = resolvedPath.fileSize(),
            children = getChildInfo(resolvedPath)
        )
    }

    override fun get(path: Path, out: OutputStream): Unit = get(path) { input ->
        input.transferTo(out)
    }

    override fun <T> get(path: Path, exe: Function<InputStream, T>): T {
        val path = root.resolve(path).full()
        checkFile(path)

        if(!path.exists()) {
            throw NotFoundException()
        }

        return path.inputStream().use { exe.apply(it) }
    }

    override fun post(path: Path, input: InputStream) {
        val resolvedPath = root.resolve(path).full()
        checkFile(resolvedPath)

        if(resolvedPath.exists()) {
            throw ConflictException()
        }

        resolvedPath.createParentDirectories()

        resolvedPath.outputStream().use { out ->
            input.transferTo(out)
            out.flush()
        }
    }

    override fun put(path: Path, input: InputStream) {
        val resolvedPath = root.resolve(path).full()
        checkFile(resolvedPath)

        resolvedPath.createParentDirectories()
        resolvedPath.outputStream().use { out ->
            input.transferTo(out)
            out.flush()
        }
    }

    override fun delete(path: Path) {
        val resolvedPath = root.resolve(path).full()
        checkFile(resolvedPath)

        if(!resolvedPath.exists()) {
            throw NotFoundException()
        }

        if(resolvedPath.isSameFileAs(root)) {
            throw ForbiddenException()
        }


        if(resolvedPath.isRegularFile()) {
            // delete file

            if(!resolvedPath.parent.isSameFileAs(root)) {
                // if the parent directory is empty and not the root directory
                // delete the directory, otherwise delete the file

                val peers = resolvedPath.parent.listChildren().filter {
                    !resolvedPath.isSameFileAs(it)
                }

                if(peers.isEmpty()) {
                    resolvedPath.parent.deleteRecursively()
                } else {
                    resolvedPath.deleteIfExists()
                }
            } else {
                // if the parent is the root directory, only delete the file
                resolvedPath.deleteIfExists()
            }
        } else if(resolvedPath.isDirectory()) {
            // delete directory
            resolvedPath.deleteRecursively()
        } else {
            // TODO error

        }
    }

    private fun getChildInfo(path: Path): List<ResourceChildInfo>? {
        if(!path.isDirectory()) {
            return null
        }

        val children = mutableListOf<ResourceChildInfo>()

        path.listChildren().forEach { child ->
            children.add(ResourceChildInfo(
                path = child.relativeTo(root),
                name = child.name,
                isDirectory = child.isDirectory(),
                size = child.fileSize(),
                lastModified = child.getLastModifiedTime().toInstant(),
            ))
        }

        return children.toList()
    }

    /**
     * Returns the Mime for the given file. If the file is a directory, `null` is returned
     */
    private fun getMime(file: Path): MediaType? {
        file.extension
        return if(file.isDirectory()) {
            null
        } else {
            return getMime(file.extension)
        }
    }

    private fun getMime(fileExtension: String): MediaType = when (fileExtension.lowercase()) {
        "xml", "pom" -> {
            MediaType.APPLICATION_XML
        }
        "md5", "sha1", "sha256", "sha512" -> {
            MediaType.TEXT_PLAIN
        }
        else -> {
            MediaType.APPLICATION_OCTET_STREAM
        }
    }

    /**
     * Runs verification against the file to make sure the user isn't trying
     * access anything outside the mvn folder
     */
    private fun checkFile(path: Path) {
        if(!path.isSubdirectoryOf(root)) {
            // users cannot access information outside the maven directory
            throw ForbiddenException()
        }
        if(path.isSymbolicLink()) {
            // supporting symbolic links poses some security concerns
            throw ForbiddenException()
        }
    }
}