package nl.tudelft.trustchain.eudivoting

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.commit
import androidx.navigation.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.eudivoting.ui.IntroFragment
import nl.tudelft.trustchain.eudivoting.ui.VotingViewModel

@AndroidEntryPoint
class VotingActivity : BaseActivity() {
    override val navigationGraph: Int = R.navigation.voting_navigation
    override val bottomNavigationMenu: Int = R.menu.voting_navigation_menu

    private lateinit var bottomNavigationView: BottomNavigationView
    private val votingViewModel: VotingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a view model for the voting activity
        votingViewModel.getProposals()

        // Modify bottom navigation activity to only appear in main fragments
        bottomNavigationView = findViewById(R.id.bottomNavigation)
        findNavController(R.id.navHostFragment).addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.introFragment, R.id.proposalListFragment, R.id.voteResultsFragment -> {
                    bottomNavigationView.visibility = BottomNavigationView.VISIBLE
                }
                else -> {
                    bottomNavigationView.visibility = BottomNavigationView.GONE
                }
            }
        }

    }

}
