package nl.tudelft.trustchain.smartphonedemocracy.data.cache

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.smartphonedemocracy.data.models.ElectionStatus
import nl.tudelft.trustchain.smartphonedemocracy.data.models.TallyInfo
import nl.tudelft.trustchain.smartphonedemocracy.data.models.VoteType
import java.math.BigInteger
import java.time.Instant

@ProvidedTypeConverter
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromElectionStatus(status: ElectionStatus): String {
        return status.name
    }

    @TypeConverter
    fun toElectionStatus(value: String): ElectionStatus {
        return ElectionStatus.valueOf(value)
    }

    @TypeConverter
    fun fromInstant(value: Long): Instant {
        return Instant.ofEpochMilli(value)
    }

    @TypeConverter
    fun toInstant(value: Instant): Long {
        return value.toEpochMilli()
    }

    @TypeConverter
    fun fromVoteType(value: VoteType): String {
        return Json.encodeToString(VoteType.serializer(), value)
    }

    @TypeConverter
    fun toVoteType(value: String): VoteType {
        return Json.decodeFromString(VoteType.serializer(), value)
    }

    @TypeConverter
    fun fromTallyInfo(info: TallyInfo): String {
        return Json.encodeToString(TallyInfo.serializer(), info)
    }

    @TypeConverter
    fun toTallyInfo(value: String): TallyInfo {
        return Json.decodeFromString(TallyInfo.serializer(), value)
    }

    @TypeConverter
    fun fromBigInteger(value: BigInteger): String {
        return value.toString()
    }

    @TypeConverter
    fun toBigInteger(value: String): BigInteger {
        return BigInteger(value)
    }
}
