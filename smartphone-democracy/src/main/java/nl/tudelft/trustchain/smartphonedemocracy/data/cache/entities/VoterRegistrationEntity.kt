package nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "voter_registrations",
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
data class VoterRegistrationEntity(
    val electionId: UUID,
    val voterNullifier: String,
    val voterCommitment: String,
    val voterPublicDecryptionKey: String,
    val zkpNullifierProof: String,
    val timestamp: Long = System.currentTimeMillis()
)
