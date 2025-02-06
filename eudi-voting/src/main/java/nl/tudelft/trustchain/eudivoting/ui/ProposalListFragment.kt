package nl.tudelft.trustchain.eudivoting.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import nl.tudelft.trustchain.eudivoting.data.TestDataGenerator
import nl.tudelft.trustchain.eudivoting.ui.compose.ProposalDetailScreen
import nl.tudelft.trustchain.eudivoting.ui.compose.ProposalListScreen

@AndroidEntryPoint
class ProposalListFragment : Fragment() {

//    private val viewModel: VotingViewModel by viewModels()

    private val testProposals = TestDataGenerator.generateProposals(16)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "proposalList") {
                    composable("proposalList") {
                        ProposalListScreen(navController = navController)
                    }
                    composable("proposalDetail/{proposalId}") { backStackEntry ->
                        val proposalId =
                            backStackEntry.arguments?.getString("proposalId")?.toIntOrNull()
                        val proposal = testProposals.find { it.id == proposalId }
                        if (proposal != null) {
                            ProposalDetailScreen(proposal)
                        }
                    }
                }
            }
        }
    }
}


