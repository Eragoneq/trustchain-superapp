package nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.voterregistration

import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.SmartphoneDemocracyCommunity
import javax.inject.Inject

data class VoterRegistrationOperation(
    val electionId: String,
    val voterNullifier: String,
    val voterCommitment: String,
    val voterPublicDecryptionKey: String,
    val zkpNullifierProof: String,
) {

    fun toTrustChainTransaction(): TrustChainTransaction {
        return mapOf(
            "electionId" to electionId,
            "voterNullifier" to voterNullifier,
            "voterCommitment" to voterCommitment,
            "voterPublicDecryptionKey" to voterPublicDecryptionKey,
            "zkpNullifierProof" to zkpNullifierProof
        )
    }

    companion object {
        const val BLOCK_TYPE = "voter_registration"

        fun fromTrustChainTransaction(transaction: TrustChainTransaction): VoterRegistrationOperation {
            return VoterRegistrationOperation(
                electionId = transaction["electionId"] as String,
                voterNullifier = transaction["voterNullifier"] as String,
                voterCommitment = transaction["voterCommitment"] as String,
                voterPublicDecryptionKey = transaction["voterPublicDecryptionKey"] as String,
                zkpNullifierProof = transaction["zkpNullifierProof"] as String
            )
        }
    }

    class Validator
    @Inject
    constructor() : TransactionValidator {
        override fun validate(
            block: TrustChainBlock,
            database: TrustChainStore
        ): ValidationResult {
            return if (validate(block)) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(listOf("Not all information included."))
            }
        }

        private fun validate(block: TrustChainBlock): Boolean {
            return validateTransaction(block.transaction)
        }

        fun validateTransaction(transaction: TrustChainTransaction): Boolean {
            val electionId = transaction["electionId"]
            val voterNullifier = transaction["voterNullifier"]
            val voterCommitment = transaction["voterCommitment"]
            val voterPublicDecryptionKey = transaction["voterPublicDecryptionKey"]
            val zkpNullifierProof = transaction["zkpNullifierProof"]
            return (
                electionId is String && electionId.isNotEmpty() && transaction.containsKey("electionId") &&
                    voterNullifier is String && voterNullifier.isNotEmpty() && transaction.containsKey(
                    "voterNullifier"
                ) &&
                    voterCommitment is String && voterCommitment.isNotEmpty() && transaction.containsKey(
                    "voterCommitment"
                ) &&
                    voterPublicDecryptionKey is String && voterPublicDecryptionKey.isNotEmpty() && transaction.containsKey(
                    "voterPublicDecryptionKey"
                ) &&
                    zkpNullifierProof is String && zkpNullifierProof.isNotEmpty() && transaction.containsKey(
                    "zkpNullifierProof"
                )
                )
        }
    }

    class Signer
    @Inject
    constructor(
        val smartphoneDemocracyCommunity: SmartphoneDemocracyCommunity
    ) : BlockSigner {
        override fun onSignatureRequest(block: TrustChainBlock) {
            smartphoneDemocracyCommunity.createAgreementBlock(block, mapOf<Any?, Any?>())
        }
    }

}
