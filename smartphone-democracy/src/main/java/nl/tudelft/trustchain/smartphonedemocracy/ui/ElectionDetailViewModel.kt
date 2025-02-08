package nl.tudelft.trustchain.smartphonedemocracy.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import nl.tudelft.trustchain.smartphonedemocracy.data.ElectionRepository
import nl.tudelft.trustchain.smartphonedemocracy.data.VoterRepository
import nl.tudelft.trustchain.smartphonedemocracy.data.models.Election
import nl.tudelft.trustchain.smartphonedemocracy.data.models.Vote
import nl.tudelft.trustchain.smartphonedemocracy.network.AuthenticationStatus
import nl.tudelft.trustchain.smartphonedemocracy.network.ElectionAuthenticationManager
import javax.inject.Inject

@HiltViewModel
class ElectionDetailViewModel @Inject constructor(
    private val electionRepository: ElectionRepository,
    private val voterRepository: VoterRepository,
    private val electionAuthManager: ElectionAuthenticationManager
) : ViewModel() {

    private val TAG = "ElectionDetailViewModel"

    // Expose authentication status from the manager
    val authStatus: StateFlow<Map<String, AuthenticationStatus>> =
        electionAuthManager.authenticationStatus

    fun getElectionFlow(electionId: String): StateFlow<Election?> {
        return electionRepository.getElectionFlow(electionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun castVote(electionId: String, vote: Vote) {
        // TODO: Implement vote casting with proper encryption and ZKP
        voterRepository.castVote(
            electionId = electionId,
            voterNullifier = "nullifier_${(0..1000).random()}",
            vote = vote,
            zkpVoteProof = "proof_${(0..1000).random()}"
        )
    }

    fun authenticateForElection(electionId: String, onUriReady: (Uri) -> Unit) {
        Log.d(TAG, "Starting authentication for election: $electionId")
        electionAuthManager.authenticateForElection(electionId, viewModelScope, onUriReady)
    }
}
