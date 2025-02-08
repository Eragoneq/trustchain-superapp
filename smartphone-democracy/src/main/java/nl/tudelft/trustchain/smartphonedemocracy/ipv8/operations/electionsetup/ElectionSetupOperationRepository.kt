package nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.electionsetup

import android.util.Log
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.SmartDemDatabase
import nl.tudelft.trustchain.smartphonedemocracy.data.models.Election
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.SmartphoneDemocracyCommunity
import javax.inject.Inject

class ElectionSetupOperationRepository @Inject constructor(
    private val smartphoneDemocracyCommunity: SmartphoneDemocracyCommunity,
    private val electionSetupOperationValidator: ElectionSetupOperation.Validator,
    private val database: SmartDemDatabase
) {

    suspend fun process(transaction: TrustChainTransaction) {
        val operation = ElectionSetupOperation.fromTrustChainTransaction(transaction)
        val election = Election.fromSetupOperation(operation)
        val el = database.electionDao.getElection(election.id)

        if (el != null) {
            Log.e(
                "ElectionSetupOperationRepository",
                "process: Election already exists (this should not happen)"
            )
        } else {
            Log.d(
                "ElectionSetupOperationRepository",
                "process: Received a new election with id: ${election.id}"
            )
            Log.d("ElectionSetupOperationRepository", "process: $election")
            database.electionDao.insertElection(election.toElectionEntity())
        }
    }

    suspend fun reprocessAll() {
        val blocks =
            smartphoneDemocracyCommunity.database.getBlocksWithType(ElectionSetupOperation.BLOCK_TYPE)
                .sortedByDescending { it.sequenceNumber }
        for (block in blocks) {
            process(block.transaction)
        }
    }

    fun get(electionId: String): ElectionSetupOperation? {
        val blocks =
            smartphoneDemocracyCommunity.database.getBlocksWithType(ElectionSetupOperation.BLOCK_TYPE)
                .filter { electionSetupOperationValidator.validateTransaction(it.transaction) } // This should be redundant
                .filter { it.transaction["electionId"] == electionId }
                .sortedByDescending { it.sequenceNumber }
                .take(1)

        return if (blocks.isNotEmpty()) {
            ElectionSetupOperation.fromTrustChainTransaction(blocks[0].transaction)
        } else {
            null
        }
    }

    fun getAll(): List<ElectionSetupOperation> {
        val blocks =
            smartphoneDemocracyCommunity.database.getBlocksWithType(ElectionSetupOperation.BLOCK_TYPE)
                .filter { electionSetupOperationValidator.validateTransaction(it.transaction) } // This should be redundant
                .sortedByDescending { it.sequenceNumber }

        return blocks.map { ElectionSetupOperation.fromTrustChainTransaction(it.transaction) }
    }

    fun publish(operation: ElectionSetupOperation): TrustChainBlock? {
        val transaction = operation.toTrustChainTransaction()

        if (!electionSetupOperationValidator.validateTransaction(transaction)) {
            return null
        }

        Log.d("ElectionSetupBlockRepository", "publish: $transaction")
        return smartphoneDemocracyCommunity.createProposalBlock(
            blockType = ElectionSetupOperation.BLOCK_TYPE,
            transaction = transaction,
            publicKey = smartphoneDemocracyCommunity.myPeer.publicKey.keyToBin()
        )
    }
}
