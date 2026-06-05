package com.example.network

import android.util.Log
import com.example.data.HandshakeHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.*

object SpoofEngine {
    private const val TAG = "SpoofEngine"

    /**
     * Attempts a custom TLS handshake with SNI spoofing.
     * Connects to [targetHost]:[targetPort] but sets the TLS Server Name Indication (SNI) extension to [spoofedSni].
     * 
     * To fully support educational SNI diagnostics, we can either use a trust-all context or a standard platform SSLContext.
     * We'll capture certificate details, protocol version, cipher suite, and latency.
     */
    suspend fun testSniSpoof(
        targetHost: String,
        targetPort: Int,
        spoofedSni: String,
        trustAll: Boolean = true
    ): HandshakeHistory = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var socket: Socket? = null
        var sslSocket: SSLSocket? = null
        
        try {
            // 1. Establish underlying TCP socket
            socket = Socket()
            socket.connect(InetSocketAddress(targetHost, targetPort), 10000) // 10 second timeout

            // 2. Build SSLContext
            val sslContext = if (trustAll) {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                    override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                })
                SSLContext.getInstance("TLS").apply {
                    init(null, trustAllCerts, java.security.SecureRandom())
                }
            } else {
                SSLContext.getDefault()
            }

            // 3. Create SSLSocket layered over TCP socket
            // Crucial: Use the auto-close flag so closing sslSocket closes underlying TCP socket.
            val factory = sslContext.socketFactory
            sslSocket = factory.createSocket(socket, targetHost, targetPort, true) as SSLSocket

            // 4. Configure SNI (Server Name Indication) using standard SNIServerName
            val sslParams = sslSocket.sslParameters
            val sniServerName: SNIServerName = SNIHostName(spoofedSni)
            sslParams.serverNames = listOf(sniServerName)
            sslSocket.sslParameters = sslParams

            // 5. Trigger SSL Handshake
            sslSocket.startHandshake()

            val latency = System.currentTimeMillis() - startTime
            val session = sslSocket.session

            val certs = session.peerCertificates
            val peerCert = certs.firstOrNull() as? X509Certificate

            val issuer = peerCert?.issuerX500Principal?.name ?: "Unknown Issuer"
            val subject = peerCert?.subjectX500Principal?.name ?: "Unknown Subject"
            
            HandshakeHistory(
                targetHost = targetHost,
                targetPort = targetPort,
                spoofedSni = spoofedSni,
                isSuccess = true,
                latencyMs = latency,
                errorMessage = null,
                certIssuer = issuer,
                certSubject = subject,
                cipherSuite = session.cipherSuite,
                protocol = session.protocol
            )
        } catch (e: Exception) {
            Log.e(TAG, "SNI Spoofing Handshake Failed", e)
            val latency = System.currentTimeMillis() - startTime
            
            HandshakeHistory(
                targetHost = targetHost,
                targetPort = targetPort,
                spoofedSni = spoofedSni,
                isSuccess = false,
                latencyMs = latency,
                errorMessage = e.localizedMessage ?: e.javaClass.simpleName,
                certIssuer = null,
                certSubject = null,
                cipherSuite = null,
                protocol = null
            )
        } finally {
            try {
                sslSocket?.close()
            } catch (ignored: Exception) {}
            try {
                socket?.close()
            } catch (ignored: Exception) {}
        }
    }
}
