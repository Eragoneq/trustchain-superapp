package nl.tudelft.trustchain.smartphonedemocracy.crypto.models

/**
 * Represents a Zero-Knowledge Proof
 * @property proof The actual proof bytes
 * @property publicInputs The public inputs used in the proof
 */
data class ZKPProof(
    val proof: ByteArray,
    val publicInputs: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ZKPProof

        if (!proof.contentEquals(other.proof)) return false
        if (!publicInputs.contentEquals(other.publicInputs)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = proof.contentHashCode()
        result = 31 * result + publicInputs.contentHashCode()
        return result
    }
}

/**
 * Represents a cryptographic commitment
 * @property commitment The commitment bytes
 */
data class Commitment(
    val commitment: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Commitment

        return commitment.contentEquals(other.commitment)
    }

    override fun hashCode(): Int = commitment.contentHashCode()
}

/**
 * Represents the final plaintext tally result
 * @property result The decrypted tally result
 */
data class PlaintextTally(
    val result: Int
)
