package org.ivcode.mvn.util

import java.io.File

public fun File.validateExists(msg: String = "file does not exist: ${this.path}"): File = apply {
    if(!exists()) {
        throw IllegalArgumentException(msg)
    }
}

public fun File.validateIsFile(msg: String = "file does not represent a file: ${this.path}"): File = apply {
    if(!isFile) {
        throw IllegalArgumentException(msg)
    }
}
