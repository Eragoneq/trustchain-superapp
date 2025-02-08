package nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.voterregistration

import android.util.Log
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.SmartDemDatabase
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities.VoterRegistrationEntity
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.SmartphoneDemocracyCommunity
import java.util.UUID
import javax.inject.Inject

class VoterRegistrationOperationRepository @Inject constructor(
    private val smartphoneDemocracyCommunity: SmartphoneDemocracyCommunity,
    private val voterRegistrationOperationValidator: VoterRegistrationOperation.Validator,
    private val database: SmartDemDatabase
) {
    fun get(electionId: String, voterNullifier: String): VoterRegistrationOperation? {
        val blocks =
            smartphoneDemocracyCommunity.database.getBlocksWithType(VoterRegistrationOperation.BLOCK_TYPE)
                .filter { voterRegistrationOperationValidator.validateTransaction(it.transaction) }
                .filter {
                    it.transaction["electionId"] == electionId &&
                        it.transaction["voterNullifier"] == voterNullifier
                }
                .sortedByDescending { it.sequenceNumber }
                .take(1)

        return if (blocks.isNotEmpty()) {
            VoterRegistrationOperation.fromTrustChainTransaction(blocks[0].transaction)
        } else {
            null
        }
    }

    fun getByElectionId(electionId: String): List<VoterRegistrationOperation> {
        val blocks =
            smartphoneDemocracyCommunity.database.getBlocksWithType(VoterRegistrationOperation.BLOCK_TYPE)
                .filter { voterRegistrationOperationValidator.validateTransaction(it.transaction) }
                .filter { it.transaction["electionId"] == electionId }
                .sortedByDescending { it.sequenceNumber }

        return blocks.map { VoterRegistrationOperation.fromTrustChainTransaction(it.transaction) }
    }

    fun publish(operation: VoterRegistrationOperation): TrustChainBlock? {
        val transaction = operation.toTrustChainTransaction()

        if (!voterRegistrationOperationValidator.validateTransaction(transaction)) {
            return null
        }

        Log.d("VoterRegistrationBlockRepository", "publish: $transaction")
        return smartphoneDemocracyCommunity.createProposalBlock(
            blockType = VoterRegistrationOperation.BLOCK_TYPE,
            transaction = transaction,
            publicKey = smartphoneDemocracyCommunity.myPeer.publicKey.keyToBin()
        )
    }

    suspend fun process(transaction: TrustChainTransaction) {
        val operation = VoterRegistrationOperation.fromTrustChainTransaction(transaction)
        val registrationEntity = VoterRegistrationEntity(
            electionId = UUID.fromString(operation.electionId),
            voterNullifier = operation.voterNullifier,
            voterCommitment = operation.voterCommitment,
            voterPublicDecryptionKey = operation.voterPublicDecryptionKey,
            zkpNullifierProof = operation.zkpNullifierProof,
        )

        val existing = database.dao.getVoterRegistration(
            registrationEntity.electionId,
            registrationEntity.voterNullifier
        )

        if (existing != null) {
            Log.e(
                "VoterRegistrationOperationRepository",
                "process: Voter registration already exists (this should not happen)"
            )
        } else {
            Log.d(
                "VoterRegistrationOperationRepository",
                "process: Received a new voter registration for electionId: ${operation.electionId}, voterNullifier: ${operation.voterNullifier}"
            )
            val election = database.electionDao.getElection(registrationEntity.electionId)
            if (election == null) {
                Log.e("VoterRegistrationOperationRepository", "process: Election does not exist")
                return
            }
            database.dao.insertVoterRegistration(registrationEntity)
        }
    }

    suspend fun reprocessAll() {
        val blocks =
            smartphoneDemocracyCommunity.database.getBlocksWithType(VoterRegistrationOperation.BLOCK_TYPE)
                .sortedByDescending { it.sequenceNumber }
        for (block in blocks) {
            process(block.transaction)
        }
    }
}

