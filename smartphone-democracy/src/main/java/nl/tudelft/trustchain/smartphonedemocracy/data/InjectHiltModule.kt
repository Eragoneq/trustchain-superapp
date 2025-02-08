package nl.tudelft.trustchain.smartphonedemocracy.data

import android.content.Context
import androidx.room.Room
import com.google.gson.FieldNamingPolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.gson.gson
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.smartphonedemocracy.crypto.CryptoUtils
import nl.tudelft.trustchain.smartphonedemocracy.crypto.CryptographyProvider
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.Converters
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.SmartDemDatabase
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.SmartphoneDemocracyCommunity
import nl.tudelft.trustchain.smartphonedemocracy.network.ElectionAuthenticationManager
import nl.tudelft.trustchain.smartphonedemocracy.network.EudiVerifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class InjectHiltModule {

    @Provides
    @Singleton
    fun eudiVoteCommunity(): SmartphoneDemocracyCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("EudiVoteCommunity is not configured")
    }

    @Provides
    @Singleton
    fun trustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    @Provides
    @Singleton
    fun eudiVerifier(
        cryptoProvider: CryptoUtils,
        httpClient: HttpClient
    ): EudiVerifier {
        return EudiVerifier(
            "https://verifier.eragon.tech",
            "verifier.eragon.tech",
            cryptoProvider,
            httpClient
        )
    }

    @Provides
    @Singleton
    fun electionAuthenticationManager(
        eudiVerifier: EudiVerifier,
        cryptoUtils: CryptoUtils,
        voterRepository: VoterRepository
    ): ElectionAuthenticationManager {
        return ElectionAuthenticationManager(eudiVerifier, voterRepository, cryptoUtils)
    }

    @Provides
    @Singleton
    fun httpClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                gson {
                    setPrettyPrinting()
                    setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
        }
    }

    @Provides
    @Singleton
    fun crypto(): CryptographyProvider {
        return CryptographyProvider()
    }

    @Provides
    @Singleton
    fun cryptoUtils(): CryptoUtils {
        return CryptoUtils()
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext applicationContext: Context
    ): SmartDemDatabase {
        return Room.databaseBuilder(
            applicationContext,
            SmartDemDatabase::class.java,
            "smart-dem-database"
        ).fallbackToDestructiveMigration()
            .addTypeConverter(Converters())
            .build()
    }

}
