package org.ivcode.mvn.controllers

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/publish"])
public class PublishController {

    @PostMapping("/{repo}/{groupId}/{artifactId}/{version}")
    public fun publish(
        @PathVariable repo: String,
        @PathVariable groupId: String,
        @PathVariable artifactId: String,
        @PathVariable version: String,
    ) {

    }
}
