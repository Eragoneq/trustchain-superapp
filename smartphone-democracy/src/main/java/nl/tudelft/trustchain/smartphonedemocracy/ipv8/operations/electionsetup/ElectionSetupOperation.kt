package nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.electionsetup

import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.SmartphoneDemocracyCommunity
import javax.inject.Inject

data class ElectionSetupOperation(
    val electionId: String,
    val electionName: String,
    val electionDescription: String,
    val electionStartDate: String,
    val electionEndDate: String,
    val electionType: String,
    val zkpParameters: String,
) {

    fun toTrustChainTransaction(): TrustChainTransaction {
        return mapOf(
            "electionId" to this.electionId,
            "electionName" to this.electionName,
            "electionDescription" to this.electionDescription,
            "electionStartDate" to this.electionStartDate,
            "electionEndDate" to this.electionEndDate,
            "electionType" to this.electionType,
            "zkpParameters" to this.zkpParameters,
        )
    }

    companion object {
        const val BLOCK_TYPE = "election_setup"

        fun fromTrustChainTransaction(transaction: TrustChainTransaction): ElectionSetupOperation {
            return ElectionSetupOperation(
                electionId = transaction["electionId"] as String,
                electionName = transaction["electionName"] as String,
                electionDescription = transaction["electionDescription"] as String,
                electionStartDate = transaction["electionStartDate"] as String,
                electionEndDate = transaction["electionEndDate"] as String,
                electionType = transaction["electionType"] as String,
                zkpParameters = transaction["zkpParameters"] as String,
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
            val electionName = transaction["electionName"]
            val electionDescription = transaction["electionDescription"]
            val electionStartDate = transaction["electionStartDate"]
            val electionEndDate = transaction["electionEndDate"]
            val electionType = transaction["electionType"]
            val zkpParameters = transaction["zkpParameters"]

            // This is an overkill and the containsKey can be removed
            return (
                electionId is String && electionId.isNotEmpty() && transaction.containsKey("electionId") &&
                    electionName is String && electionName.isNotEmpty() && transaction.containsKey("electionName") &&
                    electionDescription is String && electionDescription.isNotEmpty() && transaction.containsKey(
                    "electionDescription"
                ) &&
                    electionStartDate is String && electionStartDate.isNotEmpty() && transaction.containsKey(
                    "electionStartDate"
                ) &&
                    electionEndDate is String && electionEndDate.isNotEmpty() && transaction.containsKey(
                    "electionEndDate"
                ) &&
                    electionType is String && electionType.isNotEmpty() && transaction.containsKey("electionType") &&
                    zkpParameters is String && zkpParameters.isNotEmpty() && transaction.containsKey(
                    "zkpParameters"
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



