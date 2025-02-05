package nl.tudelft.trustchain.eudivoting.data

class InMemoryProposalRepository: ProposalRepository {

    private val proposals = mutableListOf<Proposal>()

    override fun addProposal(proposal: Proposal) {
        proposals.add(proposal)
    }

    override fun getProposal(proposalId: Int): Proposal? {
        return proposals.find { it.id == proposalId }
    }

    override fun getAllProposals(): List<Proposal> {
        return proposals
    }

    override fun updateProposal(proposal: Proposal) {
        val index = proposals.indexOfFirst { it.id == proposal.id }
        if (index!= -1) {
            proposals[index] = proposal
        }
    }

    override fun clearProposals() {
        proposals.clear()
    }
}
