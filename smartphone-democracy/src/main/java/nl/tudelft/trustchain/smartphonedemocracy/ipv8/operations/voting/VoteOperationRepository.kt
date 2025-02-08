package nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.voting

import android.util.Log
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities.VoteEntity
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.SmartphoneDemocracyCommunity
import java.util.UUID
import javax.inject.Inject

class VoteOperationRepository @Inject constructor(
    private val smartphoneDemocracyCommunity: SmartphoneDemocracyCommunity,
    private val voteOperationValidator: VoteOperation.Validator,
    private val database: nl.tudelft.trustchain.smartphonedemocracy.data.cache.SmartDemDatabase
) {
    fun get(electionId: String, voterNullifier: String): VoteOperation? {
        val blocks =
            smartphoneDemocracyCommunity.database.getBlocksWithType(VoteOperation.BLOCK_TYPE)
                .filter { voteOperationValidator.validateTransaction(it.transaction) }
                .filter {
                    it.transaction["electionId"] == electionId &&
                        it.transaction["voterNullifier"] == voterNullifier
                }
                .sortedByDescending { it.sequenceNumber }
                .take(1)

        return if (blocks.isNotEmpty()) {
            VoteOperation.fromTrustChainTransaction(blocks[0].transaction)
        } else {
            null
        }
    }

    fun getByElectionId(electionId: String): List<VoteOperation> {
        val blocks =
            smartphoneDemocracyCommunity.database.getBlocksWithType(VoteOperation.BLOCK_TYPE)
                .filter { voteOperationValidator.validateTransaction(it.transaction) }
                .filter { it.transaction["electionId"] == electionId }
                .sortedByDescending { it.sequenceNumber }

        return blocks.map { VoteOperation.fromTrustChainTransaction(it.transaction) }
    }

    fun publish(operation: VoteOperation): TrustChainBlock? {
        val transaction = operation.toTrustChainTransaction()

        if (!voteOperationValidator.validateTransaction(transaction)) {
            return null
        }

        Log.d("VoteBlockRepository", "publish: $transaction")
        return smartphoneDemocracyCommunity.createProposalBlock(
            blockType = VoteOperation.BLOCK_TYPE,
            transaction = transaction,
            publicKey = smartphoneDemocracyCommunity.myPeer.publicKey.keyToBin()
        )
    }

    suspend fun process(transaction: TrustChainTransaction) {
        val operation = VoteOperation.fromTrustChainTransaction(transaction)

        val vote = VoteEntity(
            electionId = UUID.fromString(operation.electionId),
            voterNullifier = operation.voterNullifier,
            encryptedVote = operation.encryptedVote,
            zkpVoteProof = operation.zkpVoteProof
        )

        val existing = database.voteDao.getVote(vote.electionId, operation.voterNullifier)
        if (existing != null) {
            Log.e(
                "VoteOperationRepository",
                "process: Vote already exists (this should not happen)"
            )
        } else {
            Log.d(
                "VoteOperationRepository",
                "process: Received a new vote for electionId: ${operation.electionId}, voterNullifier: ${operation.voterNullifier}"
            )
            val election = database.electionDao.getElection(vote.electionId)
            if (election == null) {
                Log.e("VoteOperationRepository", "process: Election does not exist")
                return
            }
            database.voteDao.insertVote(vote)
        }
    }

    suspend fun reprocessAll() {
        val blocks =
            smartphoneDemocracyCommunity.database.getBlocksWithType(VoteOperation.BLOCK_TYPE)
                .sortedByDescending { it.sequenceNumber }
        for (block in blocks) {
            process(block.transaction)
        }
    }
}

