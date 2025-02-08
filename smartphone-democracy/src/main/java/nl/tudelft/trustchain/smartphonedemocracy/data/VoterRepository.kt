package nl.tudelft.trustchain.smartphonedemocracy.data

import android.util.Log
import nl.tudelft.trustchain.smartphonedemocracy.crypto.SimpleHEService
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.SmartDemDatabase
import nl.tudelft.trustchain.smartphonedemocracy.data.models.BooleanVote
import nl.tudelft.trustchain.smartphonedemocracy.data.models.Vote
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.voterregistration.VoterRegistrationOperation
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.voterregistration.VoterRegistrationOperationRepository
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.voting.VoteOperation
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.voting.VoteOperationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoterRepository
@Inject constructor(
    private val voterRegistrationBlock: VoterRegistrationOperationRepository,
    private val voteBlock: VoteOperationRepository,
    private val database: SmartDemDatabase,
    private val heService: SimpleHEService,
) {
    fun registerVoter(
        electionId: String,
        voterNullifier: String,
        voterCommitment: String,
        voterPublicDecryptionKey: String,
        zkpNullifierProof: String
    ) {
        Log.d(
            "VoterRepository",
            "Registering voter for election $electionId with nullifier $voterNullifier"
        )
        voterRegistrationBlock.publish(
            VoterRegistrationOperation(
                electionId = electionId,
                voterNullifier = voterNullifier,
                voterCommitment = voterCommitment,
                voterPublicDecryptionKey = voterPublicDecryptionKey,
                zkpNullifierProof = zkpNullifierProof
            )
        )
    }

    fun castVote(
        electionId: String,
        voterNullifier: String,
        vote: Vote,
        zkpVoteProof: String
    ) {
        val encryptedVote = when (vote) {
            is BooleanVote -> heService.encryptVote(vote)
        }

        voteBlock.publish(
            VoteOperation(
                electionId = electionId,
                voterNullifier = voterNullifier,
                encryptedVote = encryptedVote.toString(),
                zkpVoteProof = zkpVoteProof
            )
        )
    }

    suspend fun refreshCacheDatabase() {
        voterRegistrationBlock.reprocessAll()
        voteBlock.reprocessAll()
    }

}
