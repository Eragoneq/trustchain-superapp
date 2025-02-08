package nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.tally

import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.SmartphoneDemocracyCommunity
import javax.inject.Inject

data class TallyOperation(
    val electionId: String,
    val tallyResult: String,
    val zkpTallyProof: String
) {

    fun toTrustChainTransaction(): TrustChainTransaction {
        return mapOf(
            "electionId" to electionId,
            "tallyResult" to tallyResult,
            "zkpTallyProof" to zkpTallyProof
        )
    }

    companion object {
        const val BLOCK_TYPE = "tally"

        fun fromTrustChainTransaction(transaction: TrustChainTransaction): TallyOperation {
            return TallyOperation(
                electionId = transaction["electionId"] as String,
                tallyResult = transaction["tallyResult"] as String, // Adjust cast if necessary
                zkpTallyProof = transaction["zkpTallyProof"] as String
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
            val tallyResult = transaction["tallyResult"]
            val zkpTallyProof = transaction["zkpTallyProof"]
            return (
                electionId is String && electionId.isNotEmpty() && transaction.containsKey("electionId") &&
                    tallyResult is String && tallyResult.isNotEmpty() && transaction.containsKey("tallyResult") &&
                    zkpTallyProof is String && zkpTallyProof.isNotEmpty() && transaction.containsKey(
                    "zkpTallyProof"
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
            // Decide if an agreement block is needed for Tally blocks or if it's handled differently
            // For now, keeping it consistent with VoterRegistrationBlock
            smartphoneDemocracyCommunity.createAgreementBlock(block, mapOf<Any?, Any?>())
        }
    }
}
