package nl.tudelft.trustchain.eudivoting.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eudivoting.R
import nl.tudelft.trustchain.eudivoting.databinding.FragmentVoteResultsBinding
import nl.tudelft.trustchain.eudivoting.databinding.VoteResultListItemBinding

data class VoteResult(val description: String, val acceptVotes: Int, val denyVotes: Int)

class VoteResultsFragment : Fragment(R.layout.fragment_vote_results) {

    private val binding by viewBinding(FragmentVoteResultsBinding::bind)

    private val voteResults = listOf(
        VoteResult("Proposal 1: Change the app's theme", 150, 50),
        VoteResult("Proposal 2: Add a new feature", 200, 100),
        VoteResult("Proposal 3: Improve performance", 100, 20)
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = VoteResultAdapter(voteResults)
        binding.voteResultsListView.adapter = adapter
    }

    private inner class VoteResultAdapter(voteResults: List<VoteResult>) :
        ArrayAdapter<VoteResult>(requireContext(), R.layout.vote_result_list_item, voteResults) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.vote_result_list_item, parent, false)

            val itemBinding = VoteResultListItemBinding.bind(view)
            getItem(position)?.let { voteResult ->
                itemBinding.proposalDescriptionTextView.text = voteResult.description
                itemBinding.votesAcceptTextView.text = "Accept: ${voteResult.acceptVotes}"
                itemBinding.votesDenyTextView.text = "Deny: ${voteResult.denyVotes}"
            }

            return view
        }
    }
}
