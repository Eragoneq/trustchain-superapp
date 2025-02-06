package nl.tudelft.trustchain.eudivoting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.navigation.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.eudivoting.ui.IntroFragment
import nl.tudelft.trustchain.eudivoting.ui.VotingViewModel
import nl.tudelft.trustchain.eudivoting.ui.compose.HomeScreen

@AndroidEntryPoint
class VotingActivity : AppCompatActivity() {

    private val votingViewModel: VotingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        votingViewModel.fetchProposals()
        setContent {
            HomeScreen()
        }
    }
}
