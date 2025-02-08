package nl.tudelft.trustchain.smartphonedemocracy.network

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.smartphonedemocracy.crypto.CryptoUtils
import nl.tudelft.trustchain.smartphonedemocracy.data.VoterRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ElectionAuthenticationManager @Inject constructor(
    private val eudiVerifier: EudiVerifier,
    private val voterRepository: VoterRepository,
    private val cryptoUtils: CryptoUtils
) {
    private val _authenticationStatus =
        MutableStateFlow<Map<String, AuthenticationStatus>>(emptyMap())
    val authenticationStatus = _authenticationStatus.asStateFlow()

    fun authenticateForElection(
        electionId: String,
        scope: CoroutineScope,
        onUriReady: (Uri) -> Unit
    ) {
        Log.d(TAG, "Starting authentication for election: $electionId")

        // Update status to pending
        updateAuthenticationStatus(electionId, AuthenticationStatus.PENDING)

        val transactionManager = eudiVerifier.createTransactionManager(scope, onUriReady)

        scope.launch {
            transactionManager.startTransaction()

            // Observe the authentication status
            transactionManager.authenticationStatus.collect { status ->
                updateAuthenticationStatus(electionId, status)
            }
        }
    }

    private fun updateAuthenticationStatus(electionId: String, status: AuthenticationStatus) {
        val currentStatus = _authenticationStatus.value.toMutableMap()
        currentStatus[electionId] = status
        _authenticationStatus.value = currentStatus

        if (status is AuthenticationStatus.Verified) {
            voterRepository.registerVoter(
                electionId = electionId,
                voterNullifier = status.nullifier,
                voterCommitment = "",
                voterPublicDecryptionKey = "",
                zkpNullifierProof = ""
            )
        }
    }

    fun getAuthenticationStatus(electionId: String): AuthenticationStatus {
        return _authenticationStatus.value[electionId] ?: AuthenticationStatus.PENDING
    }

    companion object {
        private const val TAG = "ElectionAuthenticationManager"
    }
}
