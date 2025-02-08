package nl.tudelft.trustchain.smartphonedemocracy.data.models

import nl.tudelft.trustchain.smartphonedemocracy.crypto.HEEncryptedValue
import java.math.BigInteger

data class EncryptedVote(
    val encryptedVote: HEEncryptedValue,
    val zkpVoteProof: String
)

sealed interface Vote {
    /**
     * Converts the vote to a BigInteger representation.
     * @return The BigInteger representation of the vote.
     */
    fun toBigInteger(): BigInteger

    /**
     * Converts the vote to a string representation.
     * @return The string representation of the vote.
     */
    fun toStringRepresentation(): String {
        return this.toBigInteger().toString()
    }
}

enum class BooleanVote : Vote {
    YES,
    NO;

    override fun toBigInteger(): BigInteger {
        return when (this) {
            YES -> BigInteger.ONE
            NO -> BigInteger.ZERO
        }
    }

    override fun toStringRepresentation(): String {
        return this.name.uppercase()
    }

    companion object {
        fun fromString(value: String): BooleanVote? {
            return when (value.uppercase()) {
                "YES" -> YES
                "NO" -> NO
                else -> null
            }
        }

        fun fromBoolean(value: Boolean): BooleanVote {
            return if (value) YES else NO
        }

        fun fromBigInteger(value: BigInteger): BooleanVote? {
            return when (value) {
                BigInteger.ONE -> YES
                BigInteger.ZERO -> NO
                else -> null
            }
        }

        fun fromInt(value: Int): BooleanVote? {
            return when (value) {
                1 -> YES
                0 -> NO
                else -> null
            }
        }
    }
}

