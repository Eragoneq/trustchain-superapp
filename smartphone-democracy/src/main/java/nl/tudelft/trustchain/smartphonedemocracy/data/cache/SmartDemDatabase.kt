package nl.tudelft.trustchain.smartphonedemocracy.data.cache

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities.ElectionEntity
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities.ElectionVoteCountView
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities.ElectionVoterCountView
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities.VoteEntity
import nl.tudelft.trustchain.smartphonedemocracy.data.cache.entities.VoterRegistrationEntity

@Database(
    entities = [
        ElectionEntity::class,
        VoteEntity::class,
        VoterRegistrationEntity::class
    ],
    views = [
        ElectionVoteCountView::class,
        ElectionVoterCountView::class,
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmartDemDatabase : RoomDatabase() {
    abstract val dao: SmartDemDao
    abstract val electionDao: ElectionDao
    abstract val voteDao: VoteDao
}
