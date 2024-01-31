package org.ivcode.mvn.services.publish.git

import org.apache.maven.artifact.repository.metadata.Metadata
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.dircache.DirCacheBuilder
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.lib.ObjectInserter
import org.eclipse.jgit.lib.Repository
import org.ivcode.mvn.exceptions.BadRequestException
import org.ivcode.mvn.exceptions.InternalServerErrorException
import org.ivcode.mvn.factories.GitProvider
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
    val repositoryName = "foo"
    val repo = openRepository(File("/home/isaiah/tmp/testmvn/test"))
    val git = Git(repo)

    val gitMap = mapOf(repositoryName to git)
    val provider = GitProvider(gitMap)

    PublishServiceGitImpl(
        fileServerService = FileServerServiceFileSystemImpl(
            repositoryName = "snaphost",
            mvnRoot = Path("/home/isaiah/git/isaiah-v/mvn-it-easy/mvn/www/snapshot")),
        gitProvider = provider,
        gitRepositoryName = repositoryName,
        gitBranch= "release"
    ).publish (
        groupId = "org.ivcode",
        artifactId = "param-filter",
        version = "1.0-SNAPSHOT"
    )
}

public class PublishServiceGitImpl(
    private val fileServerService: FileServerService,
    private val gitProvider: GitProvider,
    private val gitRepositoryName: String,
    private val gitBranch: String,
): PublishService {

    override fun publish(groupId: String, artifactId: String, version: String) {
        // Pull the latest metadata. 404 thrown if not found
        val publishMetadata = readMetaDataFromMvnRepo(groupId, artifactId)

        // If the version we're adding isn't defined, error
        if(!publishMetadata.isVersioned(version)) {
            // The version isn't defined in our mvn repo
            throw BadRequestException()
        }

        // Pull the directory info for the directory containing the artifacts we're publishing
        val artifactsInfo = fileServerService.getPathInfo(toPath(groupId, artifactId, version))

        // Check the state of the directory info
        if(!artifactsInfo.isDirectory || artifactsInfo.children?.isEmpty() != false) {
            // Throw exception if the artifacts directory isn't a directory or if it's empty
            throw InternalServerErrorException()
        }

        // Everything looks good, publish to git
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
        // Get and sync on the needed git instance
        gitProvider.open(gitRepositoryName) { git ->

            // TODO (do better)
            val lastUpdated = Instant.now()

            git.repository.use { repo ->
                // checkout branch without writing files
                repo.softCheckout(gitBranch)

                // Do a soft reset (move files out of stage without reverting)
                git.reset().call()

                // A
                repo.lockDirCache().use { dc ->
                    dc.builder().use { builder ->
                        repo.newObjectInserter().use { inserter ->
                            doGitPublish(
                                git = git,
                                repo = repo,
                                dc = dc,
                                builder = builder,
                                inserter = inserter,
                                lastUpdated = lastUpdated,
                                groupId = groupId,
                                artifactId = artifactId,
                                version = version,
                                artifactsDirectory = artifactsDirectory
                            )
                        } // Close Inserter
                    } //  Close DirCacheBuilder
                } // Close DirCache
            } // Close Repository
        } // Unlock Git
    }

    private fun doGitPublish(
        git: Git,
        repo: Repository,
        dc: DirCache,
        builder: DirCacheBuilder,
        inserter: ObjectInserter,
        lastUpdated: Instant,
        groupId: String,
        artifactId: String,
        version: String,
        artifactsDirectory: ResourceInfo,
    ) {
        // Pull last checked in metadata file. If none exists, create a new one
        val gitMetadata = repo.readMetadata(groupId, artifactId) ?: createMetadata(groupId, artifactId)
        // Add the new version. Null is returned if the version already exists.
        val metadata = gitMetadata.addVersion(version, lastUpdated)

        val newEntries = mutableMapOf<String, DirCacheEntry>()

        // add metadata and checksums if the metadata is updated
        if(metadata!=null) {
            // Only add if the metadata was updated
            val data = ByteArrayOutputStream().run {
                use { MetadataXpp3Writer().write(it, metadata) }
                toByteArray()
            }

            // Metadata
            builder.addDataEntry(
                inserter = inserter,
                path = metadata.toPath().toString(),
                data = data,
                lastModified = lastUpdated
            ).run {
                newEntries.put(pathString, this)
            }

            for (hashType in MvnHashType.entries) {
                builder.addDataEntry(
                    inserter = inserter,
                    path = metadata.toPath(hashType).toString(),
                    data = hashType.hex(data).toByteArray(Charsets.UTF_8),
                    lastModified = lastUpdated
                ).run {
                    newEntries.put(pathString, this)
                }
            }
        }

        // add artifacts files
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
        inserter.flush()

        if(toStage(builder, dc, newEntries)) {
            git.commit().setMessage("${groupId}:${artifactId}:${version}").call()
        }
    }

    /**
     * Adds remaining entries and puts changes into Stage
     *
     * @param builder The builder to add the remaining entries to
     * @param dirCache Where we pull the entries from
     * @param publishEntries The entries added for publishing
     *
     * @return `true` if the new `publishEntries` updates the repo and were successfully staged
     */
    private fun toStage (
        builder: DirCacheBuilder,
        dirCache: DirCache,
        publishEntries: Map<String, DirCacheEntry>
    ): Boolean {

        // the number of published entries with no change to the file
        var duplicateCount = 0

        // add existing entries not already inserted
        for(i in 0..<dirCache.entryCount) {
            val cachedEntry = dirCache.getEntry(i)
            val newEntry = publishEntries[cachedEntry.pathString]

            if(newEntry==null) {
                // new entry not found at path, add the existing one
                builder.add(cachedEntry)
            } else if(newEntry.objectId == newEntry.objectId) {
                // Entry was added to the new entries, but it's a duplicate (no diff)
                duplicateCount++
            } else {
                // Entry was updated and added
            }
        }

        return if(publishEntries.size-duplicateCount > 0) {
            // Changes found. Stage those changes
            builder.commit()
        } else {
            false
        }
    }


    /**
     * Reads the metadata from the maven repository
     */
    private fun readMetaDataFromMvnRepo(groupId: String, artifactId: String): Metadata {
        val metadataPath = getMetadataPath(groupId, artifactId)

        return fileServerService.get(metadataPath) { input ->
            MetadataXpp3Reader().read(input)
        }
    }
}