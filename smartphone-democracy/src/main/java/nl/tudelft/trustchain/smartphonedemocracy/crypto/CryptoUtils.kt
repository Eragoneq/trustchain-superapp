package nl.tudelft.trustchain.smartphonedemocracy.crypto

import io.ktor.util.encodeBase64
import nl.tudelft.trustchain.smartphonedemocracy.crypto.models.Commitment
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for basic cryptographic operations
 */
@Singleton
class CryptoUtils @Inject constructor() {
    private val secureRandom = SecureRandom()

    /**
     * Generates a SHA-256 hash of the input data
     * @param data The data to hash
     * @return The hash as a byte array
     */
    fun hash(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }

    /**
     * Generates a cryptographic commitment using a hash-based commitment scheme
     * @param value The value to commit to
     * @param randomness The randomness to use in the commitment
     * @return A Commitment object containing the commitment bytes
     */
    fun generateCommitment(value: ByteArray, randomness: ByteArray): Commitment {
        // Concatenate value and randomness
        val combined = value + randomness
        // Hash the combined value to create the commitment
        return Commitment(hash(combined))
    }

    /**
     * Generates cryptographically secure random bytes
     * @param length The number of random bytes to generate
     * @return The random bytes
     */
    fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    /**
     * Generates a nonce (number used once) for cryptographic operations
     * @param length The length of the nonce in bytes
     * @return The nonce as a Base64-encoded string
     */
    fun generateNonce(length: Int): String {
        val nonceBytes = generateRandomBytes(length)
        return nonceBytes.encodeBase64()
    }

    /**
     * Generates a random UUID
     * @return A random UUID as a string
     */
    fun generateUUID(): String {
        return java.util.UUID.randomUUID().toString()
    }

    /**
     * Verifies a commitment against a value and randomness
     * @param commitment The commitment to verify
     * @param value The value that was committed to
     * @param randomness The randomness used in the commitment
     * @return true if the commitment is valid, false otherwise
     */
    fun verifyCommitment(commitment: Commitment, value: ByteArray, randomness: ByteArray): Boolean {
        val computedCommitment = generateCommitment(value, randomness)
        return commitment.commitment.contentEquals(computedCommitment.commitment)
    }
}
