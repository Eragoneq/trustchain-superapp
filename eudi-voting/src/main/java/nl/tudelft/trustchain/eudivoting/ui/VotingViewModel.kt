package nl.tudelft.trustchain.eudivoting.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mu.KotlinLogging
import nl.tudelft.trustchain.eudivoting.data.Proposal
import nl.tudelft.trustchain.eudivoting.data.ProposalRepository
import nl.tudelft.trustchain.eudivoting.data.TestDataGenerator
import javax.inject.Inject

@HiltViewModel
class VotingViewModel @Inject constructor(private val proposalRepository: ProposalRepository): ViewModel() {

    private val logger = KotlinLogging.logger {}
    private val proposals: List<Proposal> = proposalRepository.getAllProposals()

    init {
        logger.info { "VotingViewModel created" }
        val testProposals = TestDataGenerator.generateProposals(16)
        testProposals.forEach { proposalRepository.addProposal(it) }
    }

    fun getProposals(): List<Proposal> {
        return proposals
    }

    override fun onCleared() {
        super.onCleared()
        logger.info { "VotingViewModel cleared" }
        proposalRepository.clearProposals()
    }

}
