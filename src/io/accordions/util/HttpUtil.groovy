package io.accordions.util

import groovy.transform.Field
import io.accordions.logger.Logger

import javax.net.ssl.*
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

@Field Logger log = new Logger()

@Field TrustManager[] trustAllCerts = [
        new X509TrustManager() {
            X509Certificate[] getAcceptedIssuers() {
                return null
            }

            void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            }

            void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            }
        }
] as TrustManager[]

@Field HostnameVerifier allHostsValid = new HostnameVerifier() {
    @Override
    boolean verify(String hostname, SSLSession session) {
        return true
    }
}

HttpURLConnection getURLConnection(url) {
    def nullTrustManager = [
            checkClientTrusted: { chain, authType -> },
            checkServerTrusted: { chain, authType -> },
            getAcceptedIssuers: { null }
    ]

    def nullHostnameVerifier = [
            verify: { hostname, session -> true }
    ]

    SSLContext sc = SSLContext.getInstance("SSL")
    sc.init(null, [nullTrustManager as X509TrustManager] as TrustManager[], null)
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
    HttpsURLConnection.setDefaultHostnameVerifier(nullHostnameVerifier as HostnameVerifier)
    //SSLContext sc = SSLContext.getInstance("SSL")
    //sc.init(null, trustAllCerts, new SecureRandom())
    //HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
    //HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
    return new URL("${url}").openConnection() as HttpURLConnection
}

def get(params = [:]) {
    def charset = "UTF-8"
    def conn = null
    try {
        def url = params.url
        def headers = params.headers ?: [:]

        def nullTrustManager = [
                checkClientTrusted: { chain, authType -> },
                checkServerTrusted: { chain, authType -> },
                getAcceptedIssuers: { null }
        ]

        def nullHostnameVerifier = [
                verify: { hostname, session -> true }
        ]

        SSLContext sc = SSLContext.getInstance("SSL")
        sc.init(null, [nullTrustManager as X509TrustManager] as TrustManager[], null)
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
        HttpsURLConnection.setDefaultHostnameVerifier(nullHostnameVerifier as HostnameVerifier)
        conn = new URL("${url}").openConnection() as HttpURLConnection
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

        conn = getURLConnection(url)
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
