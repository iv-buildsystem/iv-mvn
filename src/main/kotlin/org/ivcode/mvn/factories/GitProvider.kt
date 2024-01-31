package org.ivcode.mvn.factories

import org.eclipse.jgit.api.Git
import java.util.function.Function

/**
 *
 */
public class GitProvider (
    private val gitMap: Map<String, Git>
) {

    /**
     * Opens a git repository for work.
     */
    public fun <T> open (name: String, func: Function<Git, T>): T {
        val git = gitMap[name] ?: throw IllegalArgumentException("Unknown Git Reference: $name")

        // only one process can work on a given git repo at a time
        return synchronized(git) {
            func.apply(git)
        }
    }
}