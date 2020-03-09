package io.accordions.util

import groovy.transform.Field
import io.accordions.logger.Logger

@Field Logger log = new Logger()

def get(params = [:]) {
    def charset = "UTF-8"
    def conn = null
    def headers = params.headers ?: [:]
    try {
        conn = new URL("${params.url}").openConnection() as HttpURLConnection
        conn.setRequestMethod('GET')
        for (header in headers) {
            conn.setRequestProperty("${header.key}", "${header.value}")
        }
        conn.connect()

        log.debug "responseCode: ${conn.responseCode}, responseMessage: ${conn.responseMessage}"
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            return conn.inputStream.getText(charset)
        } else {
            throw new Exception(conn.errorStream.getText(charset))
        }
    } catch (Exception e) {
        log.error e.getMessage()
        if (log.isDebugEnabled()) {
            e.printStackTrace()
        }
        throw e
    } finally {
        if (conn != null) {
            try {
                conn.close()
            } catch (ignored) {
            }
        }
    }
}

def post(params = [:]) {
    def charset = "UTF-8"
    def conn = null
    try {
        def url = params.url
        def body = params.body ?: ""
        def headers = params.headers ?: [:]
        def contentType = ObjectUtil.safeValue(params.contentType, "application/json")

        log.debug "params: ${params}"

        conn = new URL("${url}").openConnection() as HttpURLConnection
        conn.setRequestMethod('POST')
        conn.setDoOutput(true)
        conn.setRequestProperty("Content-Type", contentType as String)
        for (header in headers) {
            conn.setRequestProperty("${header.key}", "${header.value}")
        }

        def bodyWriter = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))
        bodyWriter.write(body)
        bodyWriter.close()

        log.debug "responseCode: ${conn.responseCode}, responseMessage: ${conn.responseMessage}"
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            return conn.inputStream.getText(charset)
        } else {
            throw new Exception(conn.errorStream.getText(charset))
        }
    } catch (Exception e) {
        log.error e.getMessage()
        if (log.isDebugEnabled()) {
            e.printStackTrace()
        }
        throw e
    } finally {
        if (conn != null) {
            try {
                conn.close()
            } catch (ignored) {
            }
        }
    }
}

return this
