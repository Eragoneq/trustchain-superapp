package nl.tudelft.trustchain.smartphonedemocracy.network

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.util.encodeBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.smartphonedemocracy.crypto.CryptoUtils
import org.bouncycastle.jcajce.provider.digest.SHA256
import kotlin.random.Random

class EudiTransactionManager(
    private val verifierUrl: String,
    private val clientId: String,
    private val cryptoUtils: CryptoUtils,
    private val httpClient: HttpClient,
    private val scope: CoroutineScope,
    private val intentCallback: (Uri) -> Unit
) {
    private var currentNonce: String? = null
    private var currentTransactionId: String? = null
    private var _authenticationStatus: MutableStateFlow<AuthenticationStatus> =
        MutableStateFlow(AuthenticationStatus.PENDING)
    var authenticationStatus = _authenticationStatus.asStateFlow()

    companion object {
        private const val TAG = "EudiTransactionManager"
        private const val POLLING_INTERVAL_MS = 5000L // 5 seconds
        private const val POLLING_TIMEOUT_MS = 60000L // 60 seconds
    }

    fun startTransaction() {
        Log.d(TAG, "EudiTransactionManager initialized with verifier URL: $verifierUrl")
        Log.d(TAG, "Client ID: $clientId")

        // Start the transaction manager with initial setup
        scope.launch {
            // Any initial setup can be done here if needed
            Log.d(TAG, "EudiTransactionManager launched in scope: $scope")
            val response = initTransaction()
            if (response != null) {
                Log.d(TAG, "Transaction initialized successfully: $response")
                val walletUri = buildEudiWalletUri(response)
                Log.d(TAG, "EUDI Wallet URI: $walletUri")
                intentCallback(walletUri) // Trigger the intent with the wallet URI
            } else {
                _authenticationStatus.value = AuthenticationStatus.FAILED
                Log.e(TAG, "Failed to initialize transaction.")
            }
            // Start polling for the wallet response
            val walletResponse = pollForWalletResponse()

            if (walletResponse != null) {
                Log.d(TAG, "Wallet response received: $walletResponse")
                val verificationStatus = verifyPresentation(walletResponse.vpToken[0])
                if (verificationStatus is AuthenticationStatus.Verified) {
                    _authenticationStatus.value = verificationStatus
                    Log.d(TAG, "Verification status: ${verificationStatus}")
                } else {
                    _authenticationStatus.value = AuthenticationStatus.FAILED
                    Log.e(TAG, "Failed to verify presentation.")
                }
            } else {
                _authenticationStatus.value = AuthenticationStatus.FAILED
                Log.e(TAG, "No response from wallet or polling timed out.")
            }
        }
    }

    /**
     * Initiates a new presentation request with the verifier.
     * @return VerificationResponse containing details needed to construct the wallet URI, or null on failure.
     */
    private suspend fun initTransaction(): VerificationResponse? {
        currentNonce = cryptoUtils.generateNonce(16)
        val uuid1 = cryptoUtils.generateUUID()
        val uuid2 = cryptoUtils.generateUUID()

        return try {
            val response = httpClient.post {
                url("$verifierUrl/ui/presentations")
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "type": "vp_token",
                      "presentation_definition": {
                        "id": "$uuid1",
                        "input_descriptors": [
                          {
                            "id": "$uuid2",
                            "name": "Person Identification Data (PID)",
                            "purpose": "",
                            "format": {
                              "dc+sd-jwt": {
                                "sd-jwt_alg_values": [
                                  "ES256"
                                ],
                                "kb-jwt_alg_values": [
                                  "ES256"
                                ]
                              }
                            },
                            "constraints": {
                              "fields": [
                                {
                                  "path": [
                                    "${'$'}.vct"
                                  ],
                                  "filter": {
                                    "type": "string",
                                    "const": "urn:eudi:pid:1"
                                  }
                                },
                                {
                                  "path": [
                                    "${'$'}.family_name"
                                  ],
                                  "intent_to_retain": false
                                }
                              ]
                            }
                          }
                        ]
                      },
                      "nonce": "$currentNonce",
                      "request_uri_method": "get"
                    }
                    """.trimIndent()
                )
            }
            Log.d(TAG, "Init Transaction Response Status: ${response.status}")
            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                val verificationResponse: VerificationResponse = response.body()
                currentTransactionId = verificationResponse.transactionId
                verificationResponse
            } else {
                Log.e(TAG, "Init Transaction failed: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during initTransaction: ${e.message}", e)
            null
        }
    }

    /**
     * Builds the EUDI Wallet URI from the verification response.
     * @param response The VerificationResponse from initTransaction.
     * @return Uri to be used for the wallet intent.
     */
    private fun buildEudiWalletUri(response: VerificationResponse): Uri {
        val uriString =
            "eudi-openid4vp://?client_id=${response.clientId}&request_uri=${response.requestUri}&request_uri_method=${response.requestUriMethod}"
        Log.d(TAG, "EUDI Wallet URI: $uriString")
        return uriString.toUri()
    }

    /**
     * Polls the verifier for the wallet's response.
     * @return The wallet response (e.g., vp_token) as a String, or null if timeout or error.
     */
    private suspend fun pollForWalletResponse(): VerificationStatus? {
        val transId = currentTransactionId ?: run {
            Log.e(TAG, "Transaction ID is not set. Cannot poll for wallet response.")
            return null
        }

        var elapsedTime = 0L
        while (elapsedTime < POLLING_TIMEOUT_MS) {
            try {
                Log.d(TAG, "Polling for wallet response... (Transaction ID: $transId)")
                val response = httpClient.get("$verifierUrl/ui/presentations/$transId")
                Log.d(TAG, "Poll Response Status: ${response.status}")

                if (response.status == HttpStatusCode.OK) {
                    val responseBody: VerificationStatus? = response.body()
                    if (responseBody != null) {
                        Log.d(TAG, "Wallet response received: ${responseBody.vpToken}")
                        return responseBody // This should be the JWT/vp_token or relevant data
                    }
                } else if (response.status == HttpStatusCode.BadRequest || response.status == HttpStatusCode.NoContent) {
                    // Still waiting, continue polling
                    Log.d(
                        TAG,
                        "Wallet has not responded yet, status: ${response.status}. Polling again in ${POLLING_INTERVAL_MS / 1000}s."
                    )
                } else {
                    Log.e(
                        TAG,
                        "Error polling for wallet response: ${response.status} - ${response.bodyAsText()}"
                    )
                    return null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during pollForWalletResponse: ${e.message}", e)
                return null
            }
            delay(POLLING_INTERVAL_MS)
            elapsedTime += POLLING_INTERVAL_MS
        }
        Log.w(TAG, "Polling timed out for transaction ID: $transId")
        return null
    }

    /**
     * Verifies the presentation (e.g., vp_token) with the verifier.
     * @param walletResponse The response obtained from polling (should contain the vp_token).
     * @return VerificationStatus indicating if the presentation was "verified" or "failed", or null on error.
     */
    private suspend fun verifyPresentation(walletResponseToken: String): AuthenticationStatus {
        val nonce = currentNonce ?: run {
            Log.e(TAG, "Nonce is not set. Cannot verify presentation.")
            return AuthenticationStatus.FAILED
        }

        val requestBodyJson = """
            {
              "sd_jwt_vc": "$walletResponseToken",
              "nonce": "$nonce"
            }
        """.trimIndent()

        Log.d(TAG, "Verifying presentation with body: $requestBodyJson")

        return try {
            val response = httpClient.post {
                url("$verifierUrl/utilities/validations/sdJwtVc")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(parameters {
                    append("sd_jwt_vc", walletResponseToken)
                    append("nonce", nonce)
                }))
            }
            Log.d(TAG, "Verify Presentation Response Status: ${response.status}")
            if (response.status == HttpStatusCode.OK) {
                val responseBody: VerificationFinalStatus = response.body()
                Log.d(TAG, "Verify Presentation Response Body: $responseBody")
                AuthenticationStatus.Verified(
                    SHA256.Digest().digest(Random.nextBytes(16)).encodeBase64()
                )
            } else {
                Log.e(TAG, "Verification failed: ${response.status} - ${response.bodyAsText()}")
                AuthenticationStatus.FAILED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during verifyPresentation: ${e.message}", e)
            AuthenticationStatus.FAILED
        }
    }
}

data class VerificationResponse(
    val transactionId: String,
    val clientId: String,
    val requestUri: String,
    val presentationId: String,
    val requestUriMethod: String,
)

data class VerificationStatus(
    val vpToken: List<String>,
    val presentationSubmission: Map<String, Any>,
)

data class VerificationFinalStatus(
    val familyName: String,
    val iat: Long,
    val exp: Long,
    val vct: String,
    val iss: String,
)

sealed interface AuthenticationStatus {
    data class Verified(val nullifier: String) : AuthenticationStatus
    data object FAILED : AuthenticationStatus
    data object PENDING : AuthenticationStatus
}
