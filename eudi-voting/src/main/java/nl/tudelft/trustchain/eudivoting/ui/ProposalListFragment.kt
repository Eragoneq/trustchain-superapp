package nl.tudelft.trustchain.eudivoting.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eudivoting.R
import nl.tudelft.trustchain.eudivoting.data.Proposal
import nl.tudelft.trustchain.eudivoting.data.ProposalRepository
import nl.tudelft.trustchain.eudivoting.databinding.FragmentProposalListBinding
import nl.tudelft.trustchain.eudivoting.databinding.ProposalListItemBinding
import javax.inject.Inject

@AndroidEntryPoint
class ProposalListFragment : Fragment(R.layout.fragment_proposal_list) {

    private val binding by viewBinding(FragmentProposalListBinding::bind)

    @Inject
    lateinit var proposalRepository: ProposalRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ProposalAdapter(proposalRepository.getAllProposals())
        binding.proposalListView.adapter = adapter
        binding.proposalListView.setOnItemClickListener { _, _, position, _ ->
            val proposal = adapter.getItem(position)!!
            findNavController().navigate(R.id.action_proposalListFragment_to_proposalDetailFragment, bundleOf("proposalId" to proposal.id))
        }
    }

    private inner class ProposalAdapter(proposals: List<Proposal>) :
        ArrayAdapter<Proposal>(requireContext(), R.layout.proposal_list_item, proposals) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.proposal_list_item, parent, false)

            val itemBinding = ProposalListItemBinding.bind(view)

            getItem(position)?.let { proposal ->
                itemBinding.proposalDescriptionTextView.text = "Proposal ${proposal.id}"
                itemBinding.proposalDetailsTextView.text = proposal.description
                itemBinding.proposalDeadlineTextView.text = "Deadline: ${proposal.deadline}"
                itemBinding.proposalVotesTextView.text = "Votes: ${proposal.minVotes}"
            }

            return view
        }
    }
}


