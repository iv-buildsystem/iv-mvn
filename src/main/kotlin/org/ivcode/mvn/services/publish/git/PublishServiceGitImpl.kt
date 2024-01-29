package org.ivcode.mvn.services.publish.git

import org.apache.maven.artifact.repository.metadata.Metadata
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer
import org.eclipse.jgit.lib.Repository
import org.ivcode.mvn.exceptions.BadRequestException
import org.ivcode.mvn.exceptions.InternalServerErrorException
import org.ivcode.mvn.services.fileserver.FileServerService
import org.ivcode.mvn.services.fileserver.filesystem.FileServerServiceFileSystemImpl
import org.ivcode.mvn.services.fileserver.models.ResourceInfo
import org.ivcode.mvn.services.publish.PublishService
import org.ivcode.mvn.util.*
import java.io.ByteArrayOutputStream
import java.time.Instant
import kotlin.io.path.Path

public fun main(args: Array<String>) {
    PublishServiceGitImpl(FileServerServiceFileSystemImpl(
        repositoryName = "snaphost",
        mvnRoot = Path("/home/isaiah/git/isaiah-v/mvn-it-easy/mvn/www/snapshot")
    )).publish (
        groupId = "org.ivcode",
        artifactId = "param-filter",
        version = "1.0-SNAPSHOT"
    )
}

public class PublishServiceGitImpl(
    private val fileServerService: FileServerService,
    private val gitRepository: Repository,
    private val gitBranch: String,
): PublishService {

    override fun publish(groupId: String, artifactId: String, version: String) {
        val publishMetadata = readMetaDataFromMvnRepo(groupId, artifactId)

        if(!publishMetadata.isVersioned(version)) {
            // The version isn't defined in our mvn repo
            throw BadRequestException()
        }

        val artifactsInfo = fileServerService.getPathInfo(toPath(groupId, artifactId, version))
        if(!artifactsInfo.isDirectory || artifactsInfo.children?.isEmpty() != false) {
            // Throw exception if the artifacts directory isn't a directory or if it's empty
            throw InternalServerErrorException()
        }



        gitRepository.checkout(gitBranch)
        val gitMetadata = gitRepository.readMetadata(groupId, artifactId) ?: createMetadata(groupId, artifactId)
        val updatedMetadata = gitMetadata.addVersion(version, lastUpdated)
    }

    private fun publishToGit(
        groupId: String,
        artifactId: String,
        version: String,
        artifactsDirectory: ResourceInfo,
    ) {
        val lastUpdated = Instant.now()

        gitRepository.checkout(gitBranch)
        val gitMetadata = gitRepository.readMetadata(groupId, artifactId) ?: createMetadata(groupId, artifactId)
        val updatedMetadata = gitMetadata.addVersion(version, lastUpdated)

        val dc = gitRepository.lockDirCache()
        val builder = dc.builder()

        val inserter = gitRepository.newObjectInserter()

        // stage metadata
        if(updatedMetadata!=null) {
            // Only add if the metadata was updated
            val data = ByteArrayOutputStream().run {
                use { MetadataXpp3Writer().write(it, updatedMetadata) }
                toByteArray()
            }
            builder.addDataEntry (
                inserter = inserter,
                path = updatedMetadata.toPath().toString(),
                data = data,
                lastModified = lastUpdated
            )

            // TODO create and add checksums
        }

        // stage artifacts
        for(artifact in artifactsDirectory.children!!) {
            fileServerService.get(artifact.path) {
                builder.addRawEntry(
                    inserter = inserter,
                    path = artifact.path.toString(),
                    length = artifact.size,
                    input = it
                )
            }
        }

        inserter.flush()
        //builder.commit()
    }


    private fun readMetaDataFromMvnRepo(groupId: String, artifactId: String): Metadata {
        val metadataPath = getMetadataPath(groupId, artifactId)

        return fileServerService.get(metadataPath) { input ->
            MetadataXpp3Reader().read(input)
        }
    }
}