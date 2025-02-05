package nl.tudelft.trustchain.eudivoting.data

import android.icu.util.Calendar
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Proposal(
    val id: Int = 0,
    val description: String,
    val deadline: Date,
    val minVotes: Int,
    val state: ProposalState = ProposalState.PENDING,
    val voting: Voting? = null
): Parcelable

enum class ProposalState {
    ACCEPTED, DENIED, PENDING, EXPIRED
}

interface Voting : Parcelable {
    val type: VotingType // Type of voting (e.g., FOR_AGAINST, MULTIPLE_CHOICE)
    val results: VotingResults // Results of the voting
}

enum class VotingType {
    FOR_AGAINST,
    MULTIPLE_CHOICE,
}

interface VotingResults: Parcelable {
    val totalVotes: Int
}

@Parcelize
data class ForAgainstVoting(
    override val totalVotes: Int = 0,
    var forCount: Int = 0,
    var againstCount: Int = 0
): VotingResults {
    fun addVote(vote: Boolean) {
        if (vote) {
            forCount++
        } else {
            againstCount++
        }
    }
}

// Example of a multiple choice voting result (you can extend this)
@Parcelize
data class MultipleChoiceVoting(
    override val totalVotes: Int = 0,
    val optionCounts: MutableMap<String, Int> = mutableMapOf()
): VotingResults

interface ProposalRepository {
    fun addProposal(proposal: Proposal)
    fun getProposal(proposalId: Int): Proposal?
    fun getAllProposals(): List<Proposal>
    fun updateProposal(proposal: Proposal)
    fun clearProposals()
}

object TestDataGenerator {

    fun generateProposals(count: Int): List<Proposal> {
        val proposals = mutableListOf<Proposal>()
        for (i in 1..count) {
            proposals.add(Proposal(i, "Lorem ipsum dolor sit amet, consectetur adipiscing elit.", Calendar.getInstance().apply { set(2024, 1, 31) }.time, ((31*i + 23) % 119)))
        }
        return proposals
    }

}
