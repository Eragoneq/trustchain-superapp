package nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import nl.tudelft.trustchain.smartphonedemocracy.data.models.EncryptedVote
import java.util.UUID

@Entity(
    tableName = "votes",
    foreignKeys = [
        ForeignKey(
            entity = ElectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["electionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("electionId"), Index("voterNullifier")],
    primaryKeys = ["electionId", "voterNullifier"]
)
data class VoteEntity(
    val electionId: UUID,
    val voterNullifier: String,
    val encryptedVote: String,
    val zkpVoteProof: String,
    val timestamp: Long = System.currentTimeMillis()
) {

    fun toEncryptedVote(): EncryptedVote {
        return EncryptedVote(
            encryptedVote = encryptedVote.toBigInteger(),
            zkpVoteProof = zkpVoteProof
        )
    }
}
