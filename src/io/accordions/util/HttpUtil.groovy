package io.accordions.util

import groovy.transform.Field
import io.accordions.logger.Logger

import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

@Field Logger log = new Logger()

def call() {
    def trustAllCerts = [
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                }
            }
    ] as TrustManager[]

    // Install the all-trusting trust manager
    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new SecureRandom());

    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

    def allHostsValid = new HostnameVerifier() {
        boolean verify(String hostname, SSLSession session) {
            return true
        }
    }
    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
}

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
