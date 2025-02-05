package nl.tudelft.trustchain.eudivoting.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eudivoting.R
import nl.tudelft.trustchain.eudivoting.data.Proposal
import nl.tudelft.trustchain.eudivoting.data.ProposalRepository
import nl.tudelft.trustchain.eudivoting.databinding.FragmentProposalDetailsBinding
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class ProposalDetailFragment : Fragment(R.layout.fragment_proposal_details) {

    @Inject
    lateinit var proposalRepository: ProposalRepository

    private val binding by viewBinding(FragmentProposalDetailsBinding::bind)
    private val args: ProposalDetailFragmentArgs by navArgs()
    private val logger = KotlinLogging.logger {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val proposalId = args.proposalId

        // Fetch the Proposal object using the proposalId (e.g., from a database or repository)
        val proposal = proposalRepository.getProposal(proposalId) ?: Proposal(-1,"Proposal not found", Date(), 0)

        binding.proposalTextView.text = proposal.description

        binding.acceptButton.setOnClickListener {
            Toast.makeText(context, "Accepted", Toast.LENGTH_SHORT).show()
            // Handle accept action
        }

        binding.denyButton.setOnClickListener {
            Toast.makeText(context, "Denied", Toast.LENGTH_SHORT).show()
            // Handle deny action
        }
    }
}
