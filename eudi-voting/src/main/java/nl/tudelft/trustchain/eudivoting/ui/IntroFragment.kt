package nl.tudelft.trustchain.eudivoting.ui

import android.os.Bundle
import android.view.View
import mu.KotlinLogging
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eudivoting.R
import nl.tudelft.trustchain.eudivoting.community.EudiVoteCommunity
import nl.tudelft.trustchain.eudivoting.databinding.FragmentIntroBinding


class IntroFragment : BaseFragment(R.layout.fragment_intro) {

    private val binding by viewBinding(FragmentIntroBinding::bind)
    private val logger = KotlinLogging.logger {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logger.debug { "IntroFragment onViewCreated" }

        binding.verifyUserButton.setOnClickListener {
            logger.info { "Verify user button clicked" }
        }
        logPeers()

    }

    private fun logPeers() {
        try {
            val community = IPv8Android.getInstance().getOverlay<EudiVoteCommunity>()
            if (community != null) {
                val peers = community.getPeers()
                logger.info { "Number of peers: ${peers.size}" }
                peers.forEach { peer ->
                    logger.debug { "Peer ID: ${peer.mid}" }
                }
            } else {
                logger.error { "EudiVoteCommunity overlay not found." }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error while getting peers" }
        }
    }

}
