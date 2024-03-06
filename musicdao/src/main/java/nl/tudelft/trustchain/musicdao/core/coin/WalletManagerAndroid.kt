package nl.tudelft.trustchain.musicdao.core.coin

import android.content.Context

/**
 * Singleton class for WalletManager which also sets-up Android specific things.
 */
object WalletManagerAndroid { // TODO: Clean up Thread usage.
    private var walletManager: WalletManager? = null
    var isRunning: Boolean = false

    fun getInstance(): WalletManager {
        return walletManager
            ?: throw IllegalStateException("WalletManager is not initialized")
    }

    class Factory(
        private val context: Context
    ) {
        private var configuration: WalletManagerConfiguration? = null

        fun setConfiguration(configuration: WalletManagerConfiguration): Factory {
            this.configuration = configuration
            return this
        }

        fun init(): WalletManager {
            val walletDir = context.filesDir
            val configuration =
                configuration
                    ?: throw IllegalStateException("Configuration is not set")

            val walletManager =
                WalletManager(
                    configuration,
                    walletDir,
                    configuration.key,
                    configuration.addressPrivateKeyPair
                )

            WalletManagerAndroid.walletManager = walletManager
            isRunning = true

            return walletManager
        }
    }

    fun isInitialized(): Boolean {
        return walletManager != null
    }

    /**
     * Stops and resets the current wallet manager.
     * This method will block the thread until the kit has been shut down.
     */
    fun close() {
        walletManager!!.kit.stopAsync().awaitTerminated()
        walletManager = null
    }
}
