package nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.voting

import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.SmartphoneDemocracyCommunity
import javax.inject.Inject

data class VoteOperation(
    val electionId: String,
    val voterNullifier: String,
    val encryptedVote: String,
    val zkpVoteProof: String
) {

    fun toTrustChainTransaction(): TrustChainTransaction {
        return mapOf(
            "electionId" to electionId,
            "voterNullifier" to voterNullifier,
            "encryptedVote" to encryptedVote,
            "zkpVoteProof" to zkpVoteProof
        )
    }

    companion object {
        const val BLOCK_TYPE = "vote"

        fun fromTrustChainTransaction(transaction: TrustChainTransaction): VoteOperation {
            return VoteOperation(
                electionId = transaction["electionId"] as String,
                voterNullifier = transaction["voterNullifier"] as String,
                encryptedVote = transaction["encryptedVote"] as String,
                zkpVoteProof = transaction["zkpVoteProof"] as String
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
            val encryptedVote = transaction["encryptedVote"]
            val zkpVoteProof = transaction["zkpVoteProof"]
            return (
                electionId is String && electionId.isNotEmpty() && transaction.containsKey("electionId") &&
                    voterNullifier is String && voterNullifier.isNotEmpty() && transaction.containsKey(
                    "voterNullifier"
                ) &&
                    encryptedVote is String && encryptedVote.isNotEmpty() && transaction.containsKey(
                    "encryptedVote"
                ) &&
                    zkpVoteProof is String && zkpVoteProof.isNotEmpty() && transaction.containsKey("zkpVoteProof")
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
