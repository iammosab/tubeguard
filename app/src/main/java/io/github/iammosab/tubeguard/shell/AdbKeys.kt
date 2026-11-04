package io.github.iammosab.tubeguard.shell

import android.content.Context
import android.os.Build
import android.sun.security.x509.AlgorithmId
import android.sun.security.x509.CertificateAlgorithmId
import android.sun.security.x509.CertificateExtensions
import android.sun.security.x509.CertificateIssuerName
import android.sun.security.x509.CertificateSerialNumber
import android.sun.security.x509.CertificateSubjectName
import android.sun.security.x509.CertificateValidity
import android.sun.security.x509.CertificateVersion
import android.sun.security.x509.CertificateX509Key
import android.sun.security.x509.KeyIdentifier
import android.sun.security.x509.SubjectKeyIdentifierExtension
import android.sun.security.x509.X500Name
import android.sun.security.x509.X509CertImpl
import android.sun.security.x509.X509CertInfo
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import org.conscrypt.Conscrypt
import java.io.ByteArrayInputStream
import java.io.File
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import kotlin.random.Random

/**
 * The app's own ADB identity: an RSA key and a self-signed certificate kept in filesDir/adb, so the TV's
 * "Always allow" answer keeps applying after restarts and updates.
 */
object AdbKeys {
    private const val ALGORITHM = "SHA512withRSA"
    private var manager: Manager? = null

    @Synchronized
    fun manager(context: Context): AbsAdbConnectionManager {
        manager?.let { return it }
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
        val dir = File(context.filesDir, "adb").apply { mkdirs() }
        val keyFile = File(dir, "private.key")
        val certFile = File(dir, "cert.der")
        if (!keyFile.exists() || !certFile.exists()) generate(keyFile, certFile)
        val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(keyFile.readBytes()))
        val cert = CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(certFile.readBytes()))
        return Manager(key, cert).also {
            it.setApi(Build.VERSION.SDK_INT)
            manager = it
        }
    }

    private fun generate(keyFile: File, certFile: File) {
        val gen = KeyPairGenerator.getInstance("RSA").apply { initialize(2048, SecureRandom()) }
        val pair = gen.generateKeyPair()
        val name = X500Name("CN=TubeGuard")
        val from = Date()
        val until = Date(from.time + 30L * 365 * 24 * 3600 * 1000)
        val ext = CertificateExtensions().apply {
            set("SubjectKeyIdentifier", SubjectKeyIdentifierExtension(KeyIdentifier(pair.public).identifier))
        }
        val info = X509CertInfo().apply {
            set(X509CertInfo.VERSION, CertificateVersion(CertificateVersion.V3))
            set(X509CertInfo.SERIAL_NUMBER, CertificateSerialNumber(Random.nextInt() and Int.MAX_VALUE))
            set(X509CertInfo.ALGORITHM_ID, CertificateAlgorithmId(AlgorithmId.get(ALGORITHM)))
            set(X509CertInfo.SUBJECT, CertificateSubjectName(name))
            set(X509CertInfo.KEY, CertificateX509Key(pair.public))
            set(X509CertInfo.VALIDITY, CertificateValidity(from, until))
            set(X509CertInfo.ISSUER, CertificateIssuerName(name))
            set(X509CertInfo.EXTENSIONS, ext)
        }
        val cert = X509CertImpl(info).apply { sign(pair.private, ALGORITHM) }
        keyFile.writeBytes(pair.private.encoded)
        certFile.writeBytes(cert.encoded)
    }

    private class Manager(private val key: PrivateKey, private val cert: Certificate) : AbsAdbConnectionManager() {
        override fun getPrivateKey() = key
        override fun getCertificate() = cert
        override fun getDeviceName() = "TubeGuard"
    }
}
