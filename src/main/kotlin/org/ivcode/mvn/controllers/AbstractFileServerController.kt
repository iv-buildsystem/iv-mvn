package org.ivcode.mvn.controllers

import freemarker.template.Template
import jakarta.servlet.ServletContext
import org.ivcode.mvn.services.fileserver.FileServerService
import org.ivcode.mvn.util.toFreemarkerDataModel
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path

public open abstract class AbstractFileServerController (
    private val repositoryName: String,
    private val fileServerService: FileServerService,
    private val servletContext: ServletContext,
    private val directoryTemplate: Template
) {
    public open fun get(request: RequestEntity<Any>): ResponseEntity<StreamingResponseBody> {

        val pathInfo = fileServerService.getPathInfo(getPath(request.url))

        if(pathInfo.isDirectory) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(StreamingResponseBody { out ->
                directoryTemplate.process(pathInfo.toFreemarkerDataModel(), out.writer())
            })
        }

        val stream = StreamingResponseBody { out ->
            fileServerService.get(pathInfo.path, out)
        }

        return ResponseEntity.ok().run {
            if(pathInfo.mimeType!=null)
                contentType(pathInfo.mimeType)

            body(stream)
        }
    }

    public open fun post(request: RequestEntity<InputStream>): ResponseEntity<Any> {
        request.body.use {
            fileServerService.post(getPath(request.url), it!!)
        }

        return ResponseEntity.ok().build()
    }

    public open fun put(request: RequestEntity<InputStream>): ResponseEntity<Any> {
        request.body.use {
            fileServerService.put(getPath(request.url), it!!)
        }

        return ResponseEntity.ok().build()
    }

    public open fun delete(request: RequestEntity<Any>): ResponseEntity<Any> {
        fileServerService.delete(getPath(request.url))
        return ResponseEntity.ok().build()
    }

    private fun getPath(uri: URI): Path {
        // remove the servlet context, if there is one
        val path = Path(uri.path
            .removePrefix("/")
            .removePrefix(repositoryName)
            .removePrefix("/"))
        val context = Path(servletContext.contextPath.removePrefix("/"))

        return context.relativize(path.resolve(context))
    }
}