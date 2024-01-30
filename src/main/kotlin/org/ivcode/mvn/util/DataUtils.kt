package org.ivcode.mvn.util

import org.apache.commons.codec.digest.DigestUtils

public fun ByteArray.md5Hex(): String = DigestUtils.md5Hex(this)

public fun ByteArray.sha1Hex(): String = DigestUtils.sha1Hex(this)

public fun ByteArray.sha256Hex(): String = DigestUtils.sha256Hex(this)

public fun ByteArray.sha512Hex(): String = DigestUtils.sha512Hex(this)