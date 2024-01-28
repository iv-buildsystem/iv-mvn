package org.ivcode.mvn.services

import org.ivcode.mvn.util.checkout
import org.ivcode.mvn.util.openRepository
import org.ivcode.mvn.util.readMetadata
import java.io.File

public fun main(args: Array<String>) {

    val repo = openRepository(File("/home/isaiah/git/test/test"))
    repo.checkout("burp")
    //val git = Git.open(File("/home/isaiah/git/test/test"))

//    git.checkout().setName("main").call()






    println(repo.branch)
    //val git = Git.init().setGitDir(File("/home/isaiah/git/test")).call()

    // Add metadata files

    // wait (allowing any in pregress publishes to finish)

    val metadata = repo.readMetadata("org.ivcode", "param-filter")
    println(metadata)



    /*

        val dc = git.repository.lockDirCache()
        val builder = dc.builder()

        // Iterate over the metadata files
        for(i in 0..<dc.entryCount) {
            val entry = dc.getEntry(i)
            builder.add(entry)

            // add checksums based on stashed data
            // Q: The checksums already exist. Why are we generating them?
            // A: The goal here is to only backup completed deployments. The metadata files are used to identify what
            // deployments need to be backed up. However, at any time during this process, another deploy on the same
            // artifact set could happen. To avoid pulling the wrong checksums that were saved midway
            // through deploying artifacts, we're generating them. In this we can read the meta-data. Identify the
            // artifacts from completed deployments.

            // Identify if the metadata is at the group level or is snapshot information

            // If it's a release, add the entire folder
            // The problem here is that artifacts can be redeployed.
            // In this case, pay attention to the last modified date. If any artifacts were published after the metadata
            // specifies. We may be in the middle of publishing.

            // If it's a snapshot, add the latest version defined in the metadata and everything before.

            val loader = git.repository.open(entry.objectId)
            loader.copyTo(System.out)
        }

        val inserter = git.repository.newObjectInserter()
        git.diff().setCached(true).call().forEach {
            val entry = DirCacheEntry("zoop.txt");
            val data = "${it.newPath}".toByteArray(Charsets.UTF_8)

            entry.fileMode = FileMode.REGULAR_FILE
            entry.length = data.size
            entry.setLastModified(Instant.now())

            val id = ByteArrayInputStream(data).use { out ->
                inserter.insert(Constants.OBJ_BLOB, data.size.toLong(), out)
            }
            entry.setObjectId(id)
            builder.add(entry)

            // Identify if the metadata is at the group level or is snapshot information

            // If it's a release, add the version numbers defined

            // If it's a snapshot, add the latest version defined in the metadata and everything before. Ignore everything after

            // Print
            //val loader = git.repository.open(it.newId.toObjectId())
            //loader.copyTo(System.out)
        }
        inserter.flush()

     */
    //builder.commit()


/*
    // find the HEAD
    val lastCommitId: ObjectId = git.repository.resolve("main")

    RevWalk(git.repository).use { revWalk ->
        val commit: RevCommit = revWalk.parseCommit(lastCommitId)
        // and using commit's tree find the path
        val tree = commit.tree
        println("Having tree: $tree")

        TreeWalk(git.repository).use { treeWalk ->
            treeWalk.addTree(tree)
            treeWalk.setRecursive(true)
            //treeWalk.setFilter(PathFilter.create("src/main/kotlin/org/ivcode/mvn/services/JGitBackup.kt"))

            while (treeWalk.next()) {
                println(treeWalk.pathString)
                //val objectId: ObjectId = treeWalk.getObjectId(0)
                //val loader: ObjectLoader = git.repository.open(objectId)

                // and then one can the loader to read the file
                //loader.copyTo(System.out)
            }


        }
        revWalk.dispose()
    }
*/
}
