package nl.tudelft.trustchain.eudivoting.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class VotingHiltModule {

    @Provides
    @Singleton
    fun provideProposalRepository(): ProposalRepository {
        return InMemoryProposalRepository()
    }

}
