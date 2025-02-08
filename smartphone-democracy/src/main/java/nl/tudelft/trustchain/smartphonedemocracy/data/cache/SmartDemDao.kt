package nl.tudelft.trustchain.smartphonedemocracy.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities.VoterRegistrationEntity
import java.util.UUID

@Dao
interface SmartDemDao {

    // Voter registration operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoterRegistration(registration: VoterRegistrationEntity)

    @Query("SELECT * FROM voter_registrations WHERE electionId = :electionId")
    fun getVoterRegistrationsForElectionFlow(electionId: UUID): Flow<List<VoterRegistrationEntity>>

    @Query("SELECT * FROM voter_registrations WHERE electionId = :electionId AND voterNullifier = :voterNullifier")
    fun getVoterRegistrationFlow(
        electionId: UUID,
        voterNullifier: String
    ): Flow<VoterRegistrationEntity?>

    @Query("SELECT * FROM voter_registrations WHERE electionId = :electionId AND voterNullifier = :voterNullifier")
    suspend fun getVoterRegistration(
        electionId: UUID,
        voterNullifier: String
    ): VoterRegistrationEntity?


    // Cleanup operations
    @Query("DELETE FROM voter_registrations WHERE electionId = :electionId")
    suspend fun deleteVoterRegistrationsForElection(electionId: UUID)
}
