package nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.tally

import android.util.Log
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.SmartphoneDemocracyCommunity
import java.util.UUID
import javax.inject.Inject

class TallyOperationRepository @Inject constructor(
    private val smartphoneDemocracyCommunity: SmartphoneDemocracyCommunity,
    private val tallyOperationValidator: TallyOperation.Validator,
    private val database: nl.tudelft.trustchain.smartphonedemocracy.data.cache.SmartDemDatabase
) {
    fun get(electionId: String): TallyOperation? {
        val blocks =
            smartphoneDemocracyCommunity.database.getBlocksWithType(TallyOperation.BLOCK_TYPE)
                .filter { tallyOperationValidator.validateTransaction(it.transaction) }
                .filter { it.transaction["electionId"] == electionId }
                .sortedByDescending { it.sequenceNumber }
                .take(1)

        return if (blocks.isNotEmpty()) {
            TallyOperation.fromTrustChainTransaction(blocks[0].transaction)
        } else {
            null
        }
    }

    fun getAll(): List<TallyOperation> {
        val blocks =
            smartphoneDemocracyCommunity.database.getBlocksWithType(TallyOperation.BLOCK_TYPE)
                .filter { tallyOperationValidator.validateTransaction(it.transaction) }
                .sortedByDescending { it.sequenceNumber }

        return blocks.map { TallyOperation.fromTrustChainTransaction(it.transaction) }
    }

    fun publish(operation: TallyOperation): TrustChainBlock? {
        val transaction = operation.toTrustChainTransaction()

        if (!tallyOperationValidator.validateTransaction(transaction)) {
            return null
        }

        Log.d("TallyBlockRepository", "publish: $transaction")
        return smartphoneDemocracyCommunity.createProposalBlock(
            blockType = TallyOperation.BLOCK_TYPE,
            transaction = transaction,
            publicKey = smartphoneDemocracyCommunity.myPeer.publicKey.keyToBin()
        )
    }

    suspend fun process(transaction: TrustChainTransaction) {
        val operation = TallyOperation.fromTrustChainTransaction(transaction)
        val election = database.electionDao.getElection(UUID.fromString(operation.electionId))
        if (election == null) {
            Log.e(
                "TallyOperationRepository",
                "process: Election does not exist for tally (this should not happen)"
            )
        } else {
            Log.d(
                "TallyOperationRepository",
                "process: Received a new tally for electionId: ${operation.electionId}"
            )

            //TODO: Update election tally
        }
    }
}
