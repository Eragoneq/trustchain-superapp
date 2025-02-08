package nl.tudelft.trustchain.smartphonedemocracy.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.smartphonedemocracy.crypto.CryptographyProvider
import nl.tudelft.trustchain.smartphonedemocracy.crypto.SimpleHEService
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.SmartDemDatabase
import nl.tudelft.trustchain.smartphonedemocracy.data.models.Election
import nl.tudelft.trustchain.smartphonedemocracy.data.models.ElectionStatus
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.electionsetup.ElectionSetupOperation
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.electionsetup.ElectionSetupOperationRepository
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.tally.TallyOperationRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ElectionRepository @Inject constructor(
    private val electionSetupOperationRepository: ElectionSetupOperationRepository,
    private val voterRepository: VoterRepository,
    private val tallyBlockRepository: TallyOperationRepository,
    private val cryptographyProvider: CryptographyProvider,
    private val simpleHEService: SimpleHEService,
    private val database: SmartDemDatabase,
) {

    /**
     * Publishes a new election setup to the TrustChain.
     * The electionOptions should be provided as a List<String> and will be converted
     * to a comma-separated string for storage in the block.
     */
    fun startElection(election: Election) {
        val zkpParameters = ""

        electionSetupOperationRepository.publish(
            ElectionSetupOperation(
                electionId = election.id.toString(),
                electionName = election.name,
                electionDescription = election.description,
                electionStartDate = election.startDate.toEpochMilli().toString(),
                electionEndDate = election.endDate.toEpochMilli().toString(),
                electionType = Json.encodeToString(election.type),
                zkpParameters = zkpParameters,
            )
        )
    }

    /**
     * Retrieves a single Election object by its ID.
     * It fetches the setup information and the corresponding tally information if available.
     * Returns null if no election setup is found for the given ID.
     */
    fun getElectionFlow(electionId: String): Flow<Election?> {
        val electionUUID = java.util.UUID.fromString(electionId)
        val electionFlow = combine(
            database.electionDao.getElectionFlow(electionUUID),
            database.electionDao.getElectionVoteCount(electionUUID),
            database.electionDao.getElectionVoterCount(electionUUID),
        )
        { electionEntity, voteCount, voterCount ->
            if (electionEntity != null && voteCount != null && voterCount != null) {
                electionEntity.toElection()
                    .copy(voteCount = voteCount.voteCount, voterCount = voterCount.voterCount)
            } else null
        }
        return electionFlow
    }

    /**
     * Retrieves all elections, combining setup information with tally information if available.
     */
    fun getAllElections(): Flow<List<Election>> {
        val electionFlow = combine(
            database.electionDao.getAllElectionsFlow(),
            database.electionDao.getAllElectionVoteCount(),
            database.electionDao.getAllElectionVoterCount(),
        ) { electionEntities, voteCounts, voterCounts ->
            electionEntities.map {
                val voteCount = voteCounts.find { voteCount -> voteCount.electionId == it.id }
                val voterCount = voterCounts.find { voterCount -> voterCount.electionId == it.id }
                it.toElection()
                    .copy(
                        voteCount = voteCount?.voteCount ?: 0,
                        voterCount = voterCount?.voterCount ?: 0
                    )
            }
        }
        return electionFlow
    }

    suspend fun refreshElectionStatus() {
        val now = Instant.now()
        Log.d("ElectionRepository", "Refreshing election status, now: $now")
        database.electionDao.getAllElections().map { election ->
            val status = when {
                now.isBefore(election.startDate) -> ElectionStatus.UPCOMING
                now.isAfter(election.endDate) -> ElectionStatus.CLOSED
                else -> ElectionStatus.OPEN
            }
            Log.d("ElectionRepository", "Election ${election.id} has status $status")
            if (election.status != status && election.status != ElectionStatus.ARCHIVED && election.status != ElectionStatus.TALLIED) {
                Log.d("ElectionRepository", "Updating status of election ${election.id} to $status")
                database.electionDao.updateElectionStatus(election.id, status)
            }

        }
    }

    suspend fun refreshCacheDatabase() {
        electionSetupOperationRepository.reprocessAll()
    }
}
