package nl.tudelft.trustchain.smartphonedemocracy.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.smartphonedemocracy.crypto.SimpleHEService
import nl.tudelft.trustchain.smartphonedemocracy.data.ElectionRepository
import nl.tudelft.trustchain.smartphonedemocracy.data.VoterRepository
import nl.tudelft.trustchain.smartphonedemocracy.data.models.BooleanVote
import nl.tudelft.trustchain.smartphonedemocracy.data.models.Election
import nl.tudelft.trustchain.smartphonedemocracy.data.models.ElectionStatus
import nl.tudelft.trustchain.smartphonedemocracy.data.models.VoteType
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.SmartphoneDemocracyCommunity
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val electionRepository: ElectionRepository,
    private val voterRepository: VoterRepository,
    private val community: SmartphoneDemocracyCommunity,
    private val simpleHEService: SimpleHEService,
) : ViewModel() {

    private val TAG = "DebugViewModel"

    init {
        Log.d(TAG, "DebugViewModel initialized")

        // Automatically refresh chain from peers on entering the module
        debugRefreshChainFromPeers()
    }

    // Debug/test: create election block with random data
    fun debugCreateElectionBlock() {
        Log.d(TAG, "Creating election block with random data")
        val electionId = UUID.randomUUID().toString()
        val election = Election(
            UUID.randomUUID(),
            "Test Election " + (1000..9999).random(),
            "This is a description.",
            Instant.now(),
            Instant.now().plus(1, ChronoUnit.DAYS),
            VoteType.YesNo(yesOptionName = "Yes", noOptionName = "No", publicKey = null),
            null,
            status = ElectionStatus.OPEN
        )
        electionRepository.startElection(election)
        Log.d(TAG, "Election block created with ID: $electionId")
    }

    // Debug/test: register voter block with random data
    fun debugRegisterVoterBlock(electionId: String = "debug-election") {
        val voterId = UUID.randomUUID().toString()
        voterRepository.registerVoter(
            electionId = electionId,
            voterNullifier = voterId,
            voterCommitment = "commitment_${(0..1000).random()}",
            voterPublicDecryptionKey = "public_key_${(0..1000).random()}",
            zkpNullifierProof = "proof_${(0..1000).random()}"
        )
    }

    // Debug/test: create vote block with random data
    fun debugCreateVoteBlock(electionId: String = "debug-election") {
        val voterId = UUID.randomUUID().toString()
        voterRepository.castVote(
            electionId = electionId,
            voterNullifier = voterId,
            vote = BooleanVote.fromBoolean(Random.nextBoolean()),
            zkpVoteProof = "proof_${(0..1000).random()}"
        )
    }

    // Debug/test: refresh and fetch the chain from other peers
    fun debugRefreshChainFromPeers() {
        viewModelScope.launch {
            val peers = community.getPeers()
            for (peer in peers) {
                try {
                    community.crawlChain(peer)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to crawl chain for peer: ${peer.mid}", e)
                }
            }
        }
    }

    fun debugRefreshCacheDatabase() {
        viewModelScope.launch {
            electionRepository.refreshCacheDatabase()
            voterRepository.refreshCacheDatabase()
        }
    }
}
