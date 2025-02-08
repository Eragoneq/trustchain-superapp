package nl.tudelft.trustchain.smartphonedemocracy

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.SetupSmartDemCommunity
import nl.tudelft.trustchain.smartphonedemocracy.ui.compose.VotingApp
import javax.inject.Inject

@AndroidEntryPoint
class SmartphoneDemocracyActivity : AppCompatActivity(R.layout.activity_main) {

    @Inject
    lateinit var setupSmartDemCommunity: SetupSmartDemCommunity

    override fun onCreate(savedInstanceState: Bundle?) {
        actionBar?.hide()
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            setupSmartDemCommunity.registerValidatorsAndSigners()
            setupSmartDemCommunity.registerListeners()
        }

        setContent {
            VotingApp()
        }

    }
}
