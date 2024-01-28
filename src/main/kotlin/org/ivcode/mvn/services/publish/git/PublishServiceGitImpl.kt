package org.ivcode.mvn.services.publish.git

import org.eclipse.jgit.api.Git
import org.ivcode.mvn.services.fileserver.FileServerService
import org.ivcode.mvn.services.publish.PublishService
import java.nio.file.Path
import kotlin.io.path.Path

public class PublishServiceGitImpl(
    private val fileServerService: FileServerService,
    private val git: Git
): PublishService {

    override fun publish(group: String, artifactId: String, version: String) {
        //git.repository.

    }


}