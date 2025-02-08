package nl.tudelft.trustchain.smartphonedemocracy.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities.ElectionEntity
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities.ElectionVoteCountView
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities.ElectionVoterCountView
import nl.tudelft.trustchain.smartphonedemocracy.data.models.ElectionStatus
import java.util.UUID

@Dao
interface ElectionDao {

    // Election operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElection(election: ElectionEntity)

    @Update
    suspend fun updateElection(election: ElectionEntity)

    @Query("UPDATE elections SET status = :status WHERE id = :electionId")
    suspend fun updateElectionStatus(electionId: UUID, status: ElectionStatus)

    @Query("SELECT * FROM elections")
    fun getAllElectionsFlow(): Flow<List<ElectionEntity>>

    @Query("SELECT * FROM elections")
    suspend fun getAllElections(): List<ElectionEntity>

    @Query("SELECT * FROM elections WHERE id = :electionId")
    fun getElectionFlow(electionId: UUID): Flow<ElectionEntity?>

    @Query("SELECT * FROM elections WHERE id = :electionId")
    suspend fun getElection(electionId: UUID): ElectionEntity?

    @Query("SELECT * FROM elections WHERE status = :status")
    fun getElectionsByStatusFlow(status: UUID): Flow<List<ElectionEntity>>

    // Statistics queries
    @Query("SELECT COUNT(*) FROM voter_registrations WHERE electionId = :electionId")
    fun getVoterCountFlow(electionId: UUID): Flow<Int>

    @Query("SELECT COUNT(*) FROM votes WHERE electionId = :electionId")
    fun getVoteCountFlow(electionId: UUID): Flow<Int>

    @Query("SELECT * FROM ElectionVoteCountView WHERE electionId = :electionId")
    fun getElectionVoteCount(electionId: UUID): Flow<ElectionVoteCountView?>

    @Query("SELECT * FROM ElectionVoteCountView")
    fun getAllElectionVoteCount(): Flow<List<ElectionVoteCountView>>

    @Query("SELECT * FROM ElectionVoterCountView WHERE electionId = :electionId")
    fun getElectionVoterCount(electionId: UUID): Flow<ElectionVoterCountView?>

    @Query("SELECT * FROM ElectionVoterCountView")
    fun getAllElectionVoterCount(): Flow<List<ElectionVoterCountView>>

    @Query("DELETE FROM elections WHERE id = :electionId")
    suspend fun deleteElection(electionId: UUID)

}
