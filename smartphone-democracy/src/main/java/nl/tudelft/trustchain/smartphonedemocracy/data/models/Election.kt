package nl.tudelft.trustchain.smartphonedemocracy.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.smartphonedemocracy.crypto.models.SerializablePaillierPublicKey
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities.ElectionEntity
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.operations.electionsetup.ElectionSetupOperation
import java.time.Instant
import java.util.UUID

data class Election(
    val id: UUID,
    val name: String,
    val description: String,
    val startDate: Instant,
    val endDate: Instant,
    val type: VoteType,
    val tally: TallyInfo? = null,
    val voterCount: Int = 0,
    val voteCount: Int = 0,
    val status: ElectionStatus = ElectionStatus.UPCOMING
) {
    fun toElectionEntity(): ElectionEntity {
        return ElectionEntity(
            id = id,
            name = name,
            description = description,
            startDate = startDate,
            endDate = endDate,
            type = type,
            tally = tally,
            status = status,
        )
    }

    companion object {
        fun fromSetupOperation(operation: ElectionSetupOperation): Election {
            return Election(
                id = UUID.fromString(operation.electionId),
                name = operation.electionName,
                description = operation.electionDescription,
                startDate = Instant.ofEpochMilli(operation.electionStartDate.toLong()),
                endDate = Instant.ofEpochMilli(operation.electionEndDate.toLong()),
                type = Json.decodeFromString(VoteType.serializer(), operation.electionType),
            )
        }
    }
}

@Serializable
data class TallyInfo(
    val electionId: String,
    val tallyResult: String,
    val zkpTallyProof: String
)

enum class ElectionStatus {
    UPCOMING, OPEN, CLOSED, TALLIED, ARCHIVED
}

@Serializable
sealed class VoteType {

    @Serializable
    data class YesNo(
        val yesOptionName: String = "Yes",
        val noOptionName: String = "No",
        val publicKey: SerializablePaillierPublicKey? = null
    ) : VoteType() {
        fun vote(option: Boolean): BooleanVote {
            return BooleanVote.fromBoolean(option)
        }
    }

    // For now, we only support Yes/No votes
//    data class SingleChoice(
//        val options: List<String>
//    ) : VoteType()
}
