package nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.SmartDemDatabase
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.SmartphoneDemocracyCommunity
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.electionsetup.ElectionSetupOperation
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.electionsetup.ElectionSetupOperationRepository
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.tally.TallyOperation
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.tally.TallyOperationRepository
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.voterregistration.VoterRegistrationOperation
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.voterregistration.VoterRegistrationOperationRepository
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.voting.VoteOperation
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.voting.VoteOperationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OperationCacheService @Inject constructor(
    private val electionSetupOperationRepository: ElectionSetupOperationRepository,
    private val tallyBlockRepository: TallyOperationRepository,
    private val voterRegistrationOperationRepository: VoterRegistrationOperationRepository,
    private val voteOperationRepository: VoteOperationRepository,
    private val smartphoneDemocracyCommunity: SmartphoneDemocracyCommunity,
    private val database: SmartDemDatabase,
) : BlockListener {

    // Create a reusable coroutine scope for the service
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBlockReceived(block: TrustChainBlock) {
        serviceScope.launch {
            when (block.type) {
                ElectionSetupOperation.BLOCK_TYPE -> electionSetupOperationRepository.process(block.transaction)
                TallyOperation.BLOCK_TYPE -> tallyBlockRepository.process(block.transaction)
                VoterRegistrationOperation.BLOCK_TYPE -> voterRegistrationOperationRepository.process(
                    block.transaction
                )

                VoteOperation.BLOCK_TYPE -> voteOperationRepository.process(block.transaction)
            }
        }
    }

}
