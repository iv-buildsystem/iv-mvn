package org.ivcode.mvn.util

import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink


public fun Path.full():Path =
    this.absolute().normalize()

public fun Path.isSubdirectoryOf(base: Path): Boolean =
    this.full().startsWith(base.full())

public fun Path.listChildren(): List<Path> {
    val children = mutableListOf<Path>()

    Files.walkFileTree(this, emptySet(),1, object: SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
            children.add(file)
            return FileVisitResult.CONTINUE
        }
    })

    return children
}

public fun Path.deleteRecursively() {
    Files.walkFileTree(this, object: SimpleFileVisitor<Path>() {

        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
            if(exc!=null) {
                throw exc
            }

            Files.delete(dir)
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
            Files.delete(file)
            return FileVisitResult.CONTINUE
        }
    })
}

// --== Validation ==-- //
public fun Path.validateExists(msg: String = "file does not exist: $this"): Path = apply {
    if(!exists()) {
        throw IllegalArgumentException(msg)
    }
}

public fun Path.validateIsFile(msg: String = "file does not represent a file: $this"): Path = apply {
    if(!isRegularFile()) {
        throw IllegalArgumentException(msg)
    }
}
