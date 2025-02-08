package nl.tudelft.trustchain.smartphonedemocracy.crypto

import nl.tudelft.trustchain.smartphonedemocracy.crypto.models.Commitment
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CryptoUtilsTest {
    private lateinit var cryptoUtils: CryptoUtils

    @Before
    fun setup() {
        cryptoUtils = CryptoUtils()
    }

    @Test
    fun `test hash produces consistent output`() {
        val input = "test".toByteArray()
        val hash1 = cryptoUtils.hash(input)
        val hash2 = cryptoUtils.hash(input)

        assertArrayEquals("Hash should be consistent for same input", hash1, hash2)
    }

    @Test
    fun `test hash produces different output for different inputs`() {
        val input1 = "test1".toByteArray()
        val input2 = "test2".toByteArray()
        val hash1 = cryptoUtils.hash(input1)
        val hash2 = cryptoUtils.hash(input2)

        assertFalse("Hash should be different for different inputs", hash1.contentEquals(hash2))
    }

    @Test
    fun `test generateRandomBytes produces requested length`() {
        val length = 32
        val randomBytes = cryptoUtils.generateRandomBytes(length)

        assertEquals("Random bytes should have requested length", length, randomBytes.size)
    }

    @Test
    fun `test generateRandomBytes produces different values`() {
        val length = 32
        val randomBytes1 = cryptoUtils.generateRandomBytes(length)
        val randomBytes2 = cryptoUtils.generateRandomBytes(length)

        assertFalse("Random bytes should be different each time", randomBytes1.contentEquals(randomBytes2))
    }

    @Test
    fun `test generateCommitment produces valid commitment`() {
        val value = "test".toByteArray()
        val randomness = cryptoUtils.generateRandomBytes(32)
        val commitment = cryptoUtils.generateCommitment(value, randomness)

        assertTrue("Commitment should be valid", commitment is Commitment)
        assertTrue("Commitment should verify", cryptoUtils.verifyCommitment(commitment, value, randomness))
    }

    @Test
    fun `test generateCommitment produces different commitments for same value`() {
        val value = "test".toByteArray()
        val randomness1 = cryptoUtils.generateRandomBytes(32)
        val randomness2 = cryptoUtils.generateRandomBytes(32)
        val commitment1 = cryptoUtils.generateCommitment(value, randomness1)
        val commitment2 = cryptoUtils.generateCommitment(value, randomness2)

        assertFalse("Commitments should be different for different randomness",
            commitment1.commitment.contentEquals(commitment2.commitment))
    }

    @Test
    fun `test verifyCommitment rejects invalid commitment`() {
        val value = "test".toByteArray()
        val randomness = cryptoUtils.generateRandomBytes(32)
        val commitment = cryptoUtils.generateCommitment(value, randomness)
        val wrongValue = "wrong".toByteArray()

        assertFalse("Commitment should not verify with wrong value",
            cryptoUtils.verifyCommitment(commitment, wrongValue, randomness))
    }

    @Test
    fun `test verifyCommitment rejects invalid randomness`() {
        val value = "test".toByteArray()
        val randomness = cryptoUtils.generateRandomBytes(32)
        val commitment = cryptoUtils.generateCommitment(value, randomness)
        val wrongRandomness = cryptoUtils.generateRandomBytes(32)

        assertFalse("Commitment should not verify with wrong randomness",
            cryptoUtils.verifyCommitment(commitment, value, wrongRandomness))
    }
}
