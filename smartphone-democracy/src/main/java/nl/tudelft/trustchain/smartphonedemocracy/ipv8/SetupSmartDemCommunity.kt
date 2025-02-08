package nl.tudelft.trustchain.smartphonedemocracy.ipv8

import android.util.Log
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.OperationCacheService
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.electionsetup.ElectionSetupOperation
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.tally.TallyOperation
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.voterregistration.VoterRegistrationOperation
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.voting.VoteOperation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetupSmartDemCommunity @Inject constructor(
    private val smartphoneDemocracyCommunity: SmartphoneDemocracyCommunity,
    private val electionSetupOperationValidator: ElectionSetupOperation.Validator,
    private val electionSetupOperationSigner: ElectionSetupOperation.Signer,
    private val tallyOperationValidator: TallyOperation.Validator,
    private val tallyOperationSigner: TallyOperation.Signer,
    private val voterRegistrationOperationValidator: VoterRegistrationOperation.Validator,
    private val voterRegistrationOperationSigner: VoterRegistrationOperation.Signer,
    private val voteOperationValidator: VoteOperation.Validator,
    private val voteOperationSigner: VoteOperation.Signer,
    private val cacheService: OperationCacheService,
) {

    private val TAG = "SetupSmartDemCommunity"

    fun registerValidatorsAndSigners() {
        if (smartphoneDemocracyCommunity.isValidatorAndSignerRegistered()) return
        Log.d(TAG, "Registering validators and signers")
        smartphoneDemocracyCommunity.registerTransactionValidator(
            ElectionSetupOperation.BLOCK_TYPE,
            electionSetupOperationValidator
        )
        smartphoneDemocracyCommunity.registerBlockSigner(
            ElectionSetupOperation.BLOCK_TYPE,
            electionSetupOperationSigner
        )
        smartphoneDemocracyCommunity.registerTransactionValidator(
            TallyOperation.BLOCK_TYPE,
            tallyOperationValidator
        )
        smartphoneDemocracyCommunity.registerBlockSigner(
            TallyOperation.BLOCK_TYPE,
            tallyOperationSigner
        )
        smartphoneDemocracyCommunity.registerTransactionValidator(
            VoterRegistrationOperation.BLOCK_TYPE,
            voterRegistrationOperationValidator
        )
        smartphoneDemocracyCommunity.registerBlockSigner(
            VoterRegistrationOperation.BLOCK_TYPE,
            voterRegistrationOperationSigner
        )
        smartphoneDemocracyCommunity.registerTransactionValidator(
            VoteOperation.BLOCK_TYPE,
            voteOperationValidator
        )
        smartphoneDemocracyCommunity.registerBlockSigner(
            VoteOperation.BLOCK_TYPE,
            voteOperationSigner
        )
    }

    fun registerListeners() {
        if (smartphoneDemocracyCommunity.isListenerRegistered()) return
        Log.d(TAG, "Registering listeners")
        smartphoneDemocracyCommunity.addListener(null, cacheService)
    }

}
