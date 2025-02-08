package nl.tudelft.trustchain.smartphonedemocracy.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities.VoteEntity
import java.util.UUID

@Dao
interface VoteDao {

    // Vote operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVote(vote: VoteEntity)

    @Query("SELECT * FROM votes WHERE electionId = :electionId")
    fun getVotesForElectionFlow(electionId: UUID): Flow<List<VoteEntity>>

    @Query("SELECT * FROM votes WHERE electionId = :electionId AND voterNullifier = :voterNullifier")
    fun getVoteFlow(electionId: UUID, voterNullifier: String): Flow<VoteEntity?>

    @Query("SELECT * FROM votes WHERE electionId = :electionId AND voterNullifier = :voterNullifier")
    suspend fun getVote(electionId: UUID, voterNullifier: String): VoteEntity?

    @Query("DELETE FROM votes WHERE electionId = :electionId")
    suspend fun deleteVotesForElection(electionId: UUID)
}
