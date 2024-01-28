package org.ivcode.mvn.util.auth.basicauthfile

import org.ivcode.mvn.services.auth.models.BasicAuthRole
import org.ivcode.mvn.services.auth.models.BasicAuthUserEntry

class BasicAuthUserEntryUtilsTest {

}

fun main(args: Array<String>) {
    println(BasicAuthUserEntry.hash(
        username = "user",
        role = BasicAuthRole.USER,
        password = "password"))
}