package org.ivcode.mvn.services.fileserver

import org.ivcode.mvn.services.fileserver.models.ResourceInfo
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.function.Consumer
import java.util.function.Function

public interface FileServerService {

    /**
     * Get resource / directory information
     *
     * @param path path to resource
     *
     * @throws org.ivcode.mvn.exceptions.NotFoundException if the resource or directory doesn't exist
     * @throws org.ivcode.mvn.exceptions.ForbiddenException if the path isn't allowed
     */
    public fun getPathInfo(path: Path): ResourceInfo

    /**
     * Write the contents of the resource to the given output stream
     *
     * @param resourceInfo file/directory info
     * @param out the stream to write to
     *
     * @throws org.ivcode.mvn.exceptions.NotFoundException if the resource doesn't exist or if it's a directory
     * @throws org.ivcode.mvn.exceptions.ForbiddenException if the path isn't allowed
     */
    public fun get(path: Path, out: OutputStream)
    public fun <T> get(path: Path, exe: Function<InputStream, T>): T

    /**
     * Writes the given resource to the input stream
     *
     * @param path path to resource
     * @param input stream to write resource data to
     *
     * @throws org.ivcode.mvn.exceptions.ConflictException if the resource already exists
     * @throws org.ivcode.mvn.exceptions.ForbiddenException if the path isn't allowed
     */
    public fun post(path: Path, input: InputStream)

    /**
     * Writes the given resource to the input stream, overwriting the current resource if
     * necessary
     *
     * @param path path to resource
     * @param input stream to write resource data to
     *
     * @throws org.ivcode.mvn.exceptions.ForbiddenException if the path isn't allowed
     */
    public fun put(path: Path, input: InputStream)

    /**
     * Deletes a given resource
     *
     * @param path path to resource
     * @throws org.ivcode.mvn.exceptions.ForbiddenException if the path isn't allowed
     */
    public fun delete(path: Path)
}