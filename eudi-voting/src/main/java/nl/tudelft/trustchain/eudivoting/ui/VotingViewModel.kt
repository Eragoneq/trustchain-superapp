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

    init {
        logger.info { "VotingViewModel created" }
        val testProposals = TestDataGenerator.generateProposals(16)
        testProposals.forEach { proposalRepository.addProposal(it) }
    }

    fun fetchProposals(): List<Proposal> {
        return proposalRepository.getAllProposals()
    }

    override fun onCleared() {
        super.onCleared()
        logger.info { "VotingViewModel cleared" }
        proposalRepository.clearProposals()
    }

    fun getProposal(it: Int): Proposal? {
        return proposalRepository.getProposal(it)
    }

}
