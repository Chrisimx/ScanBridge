import android.annotation.SuppressLint
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@SuppressLint("CustomX509TrustManager")
object TrustAllTM : X509TrustManager {
    @SuppressLint("TrustAllX509TrustManager")
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}

    @SuppressLint("TrustAllX509TrustManager")
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
}

fun getTrustAllTM(): Pair<SSLSocketFactory, X509TrustManager> {
    val trustAllCerts = arrayOf<TrustManager>(
        TrustAllTM
    )

    val sslContext = SSLContext.getInstance("SSL").apply {
        init(null, trustAllCerts, SecureRandom())
    }

    val trustManager = trustAllCerts[0] as X509TrustManager

    return Pair(sslContext.socketFactory, trustManager)
}
