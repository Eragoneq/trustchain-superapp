package nl.tudelft.trustchain.smartphonedemocracy.ipv8

import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCrawler
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import javax.inject.Singleton


@Singleton
class SmartphoneDemocracyCommunity(
    settings: TrustChainSettings,
    database: TrustChainStore,
    crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler) {

    override val serviceId = "51ceee9ddb55e1922e44bf6aa8e7cc809f15124b"
    private val TAG = "SmartphoneDemocracyCommunity"
    private var isListenerRegistered = false
    private var isValidatorAndSignerRegistered = false

    class Factory(
        private val settings: TrustChainSettings,
        private val database: TrustChainStore,
        private val crawler: TrustChainCrawler = TrustChainCrawler()
    ) : Overlay.Factory<SmartphoneDemocracyCommunity>(SmartphoneDemocracyCommunity::class.java) {
        override fun create(): SmartphoneDemocracyCommunity {
            return SmartphoneDemocracyCommunity(settings, database, crawler)
        }
    }

    fun isValidatorAndSignerRegistered(): Boolean {
        if (!isValidatorAndSignerRegistered) {
            isValidatorAndSignerRegistered = true
            return false
        } else {
            return true
        }
    }

    fun isListenerRegistered(): Boolean {
        if (!isListenerRegistered) {
            isListenerRegistered = true
            return false
        } else {
            return true
        }
    }

}
