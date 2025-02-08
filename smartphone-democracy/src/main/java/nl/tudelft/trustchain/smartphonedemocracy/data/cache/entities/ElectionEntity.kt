package nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities

import androidx.room.DatabaseView
import androidx.room.Entity
import androidx.room.PrimaryKey
import nl.tudelft.trustchain.smartphonedemocracy.data.models.Election
import nl.tudelft.trustchain.smartphonedemocracy.data.models.ElectionStatus
import nl.tudelft.trustchain.smartphonedemocracy.data.models.TallyInfo
import nl.tudelft.trustchain.smartphonedemocracy.data.models.VoteType
import java.time.Instant
import java.util.UUID

@Entity(tableName = "elections")
data class ElectionEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    val description: String,
    val startDate: Instant,
    val endDate: Instant,
    val type: VoteType,
    val tally: TallyInfo?,
    val status: ElectionStatus,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toElection(): Election {
        return Election(
            id = id,
            name = name,
            description = description,
            startDate = startDate,
            endDate = endDate,
            type = type,
            tally = tally,
            voterCount = 0,
            voteCount = 0,
            status = status,
        )
    }
}

// TODO: Join them together into a single view that contains all 'stats' of an election
@DatabaseView("SELECT e.id AS electionId, e.name AS electionName, COUNT(v.electionId) AS voteCount FROM elections e LEFT JOIN votes v ON e.id = v.electionId GROUP BY e.id")
data class ElectionVoteCountView(
    val electionId: UUID,
    val electionName: String,
    val voteCount: Int
)

@DatabaseView("SELECT e.id AS electionId, e.name AS electionName, COUNT(v.electionId) AS voterCount FROM elections e LEFT JOIN voter_registrations v ON e.id = v.electionId GROUP BY e.id")
data class ElectionVoterCountView(
    val electionId: UUID,
    val electionName: String,
    val voterCount: Int
)


