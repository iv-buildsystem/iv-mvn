package org.ivcode.mvn.services.publish.git

import org.apache.maven.artifact.repository.metadata.Metadata
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.lib.Repository
import org.ivcode.mvn.exceptions.BadRequestException
import org.ivcode.mvn.exceptions.InternalServerErrorException
import org.ivcode.mvn.services.fileserver.FileServerService
import org.ivcode.mvn.services.fileserver.filesystem.FileServerServiceFileSystemImpl
import org.ivcode.mvn.services.fileserver.models.ResourceInfo
import org.ivcode.mvn.services.publish.PublishService
import org.ivcode.mvn.util.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import kotlin.io.path.Path

public fun main(args: Array<String>) {
    PublishServiceGitImpl(
        fileServerService = FileServerServiceFileSystemImpl(
            repositoryName = "snaphost",
            mvnRoot = Path("/home/isaiah/git/isaiah-v/mvn-it-easy/mvn/www/snapshot")),
        gitRepository = openRepository(File("/home/isaiah/tmp/testmvn/test")),
        gitBranch= "release"
    ).publish (
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

        publishToGit(
            groupId = groupId, artifactId = artifactId, version = version,
            artifactsDirectory = artifactsInfo
        )
    }

    private fun publishToGit(
        groupId: String,
        artifactId: String,
        version: String,
        artifactsDirectory: ResourceInfo,
    ) {
        val lastUpdated = Instant.now()

        gitRepository.use {
            gitRepository.checkout(gitBranch)
            Git(gitRepository).reset().call()

            val gitMetadata = gitRepository.readMetadata(groupId, artifactId)
                ?: createMetadata(groupId, artifactId)
            val updatedMetadata = gitMetadata.addVersion(version, lastUpdated) ?: gitMetadata.apply { versioning.lastUpdated = "05" }


            gitRepository.lockDirCache().use { dc ->
                dc.builder().use { builder ->
                    gitRepository.newObjectInserter().use { inserter ->
                        val newEntries = mutableMapOf<String, DirCacheEntry>()

                        // stage metadata
                        if(updatedMetadata!=null) {
                            // Only add if the metadata was updated
                            val data = ByteArrayOutputStream().run {
                                use { MetadataXpp3Writer().write(it, updatedMetadata) }
                                toByteArray()
                            }

                            // Metadata
                            builder.addDataEntry(
                                inserter = inserter,
                                path = updatedMetadata.toPath().toString(),
                                data = data,
                                lastModified = lastUpdated
                            ).run {
                                newEntries.put(pathString, this)
                            }

                            for (hashType in MvnHashType.entries) {
                                builder.addDataEntry(
                                    inserter = inserter,
                                    path = updatedMetadata.toPath(hashType).toString(),
                                    data = hashType.hex(data).toByteArray(Charsets.UTF_8),
                                    lastModified = lastUpdated
                                ).run {
                                    newEntries.put(pathString, this)
                                }
                            }
                        }

                        // stage artifacts
                        for(artifact in artifactsDirectory.children!!) {
                            fileServerService.get(artifact.path) {
                                builder.addRawEntry(
                                    inserter = inserter,
                                    path = artifact.path.toString(),
                                    length = artifact.size,
                                    input = it
                                ).run {
                                    newEntries.put(pathString, this)
                                }
                            }
                        }

                        var noChangeCount = 0
                        // add existing entries not already inserted
                        for(i in 0..<dc.entryCount) {
                            val entry = dc.getEntry(i)
                            println(entry.pathString)
                            val newEntry = newEntries[entry.pathString]

                            if(newEntry==null) {
                                // entry wasn't added, we need to add it
                                builder.add(entry)
                            } else if(newEntry.objectId == entry.objectId) {
                                // entry is equal, no change, remove from count
                                noChangeCount++
                            }
                        }
                        inserter.flush()
                        if(newEntries.size-noChangeCount > 0) {
                            builder.commit()
                            Git(gitRepository).commit().setMessage("yo").call()
                        }
                    }
                }
            }
        }
    }


    private fun readMetaDataFromMvnRepo(groupId: String, artifactId: String): Metadata {
        val metadataPath = getMetadataPath(groupId, artifactId)

        return fileServerService.get(metadataPath) { input ->
            MetadataXpp3Reader().read(input)
        }
    }
}