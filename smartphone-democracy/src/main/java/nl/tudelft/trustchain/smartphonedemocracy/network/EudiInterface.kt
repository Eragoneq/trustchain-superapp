package nl.tudelft.trustchain.smartphonedemocracy.network

import android.net.Uri
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import nl.tudelft.trustchain.smartphonedemocracy.crypto.CryptoUtils
import javax.inject.Singleton

@Singleton
class EudiVerifier(
    private val verifierUrl: String,
    private val clientId: String,
    private var cryptoUtils: CryptoUtils,
    private var httpClient: HttpClient
) {

    fun createTransactionManager(
        scope: CoroutineScope,
        intentCallback: (Uri) -> Unit
    ): EudiTransactionManager {
        return EudiTransactionManager(
            verifierUrl = verifierUrl,
            clientId = clientId,
            cryptoUtils = cryptoUtils,
            httpClient = httpClient,
            scope = scope,
            intentCallback = intentCallback
        )
    }

}

