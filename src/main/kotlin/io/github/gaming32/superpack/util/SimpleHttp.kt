package io.github.gaming32.superpack.util

import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder

object SimpleHttp {
    private val LOGGER = LoggerFactory.getLogger(SimpleHttp::class.java)
    @JvmStatic
    val USER_AGENT: String = run {
        val specPackage = JavaUtil.SUPERPACK_CLASS.`package`
        val userAgent = "${specPackage.specificationTitle}/${specPackage.specificationVersion}"
        if (userAgent == "null/null") {
            JavaUtil.SUPERPACK_CLASS.simpleName + "/DEBUG"
        } else {
            userAgent
        }
    }

    @JvmStatic
    @Throws(MalformedURLException::class)
    fun createUrl(base: String, route: String, queryParams: Map<String, *>): URL {
        val url = StringBuilder(base)
        if (!base.endsWith("/")) {
            url.append('/')
        }
        if (!route.startsWith("/")) {
            url.append(route)
        } else {
            url.append(route, 1, route.length)
        }
        return buildUrl(url, queryParams)
    }

    @JvmStatic
    @Throws(MalformedURLException::class)
    fun createUrl(url: String, queryParams: Map<String, *>) = buildUrl(StringBuilder(url), queryParams)

    @JvmStatic
    @Throws(MalformedURLException::class)
    private fun buildUrl(url: StringBuilder, queryParams: Map<String, *>): URL {
        if (!queryParams.isEmpty()) {
            url.append('?')
            for ((key, value) in queryParams) {
                url.append(key)
                url.append('=')
                url.append(URLEncoder.encode(value.toString(), Charsets.UTF_8))
                url.append('&')
            }
            url.setLength(url.length - 1)
        }
        return URL(url.toString())
    }

    @JvmStatic
    @Throws(IOException::class)
    fun URL.request(): URLConnection {
        LOGGER.info("Requesting {}", this)
        val cnxn = openConnection()
        cnxn.setRequestProperty("User-Agent", USER_AGENT)
        return cnxn
    }

    @JvmStatic
    @Throws(IOException::class)
    fun URL.stream() = request().getInputStream()
}