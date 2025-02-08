package nl.tudelft.trustchain.smartphonedemocracy.crypto

import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierPrivateKey
import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierPublicKey
import nl.tudelft.trustchain.smartphonedemocracy.data.models.BooleanVote
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SimpleHEServiceTest {
    private lateinit var simpleHeService: SimpleHEService
    private lateinit var publicKey: PaillierPublicKey
    private lateinit var privateKey: PaillierPrivateKey

    @Before
    fun setup() {
        simpleHeService = SimpleHEService()
    }

    // This could use some fuzz testing to ensure it's not deterministic
    @Test
    fun `test encryptVote produces different ciphertexts for same vote`() {
        val voteChoice = BooleanVote.YES
        val encryptedVote1 = simpleHeService.encryptVote(voteChoice)
        val encryptedVote2 = simpleHeService.encryptVote(voteChoice)

        assertFalse("Encrypted votes should be different due to randomness",
            encryptedVote1 == encryptedVote2
        )
    }

    @Test
    fun `test encryptVote produces valid ciphertext`() {
        val voteChoice = BooleanVote.YES
        val encryptedVote = simpleHeService.encryptVote(voteChoice)

        // Check that the encrypted vote is not null and is a valid BigInteger
        assertNotNull("Encrypted vote should not be null", encryptedVote)
    }

    // This could use some fuzz testing to ensure it's not flaky
    @Test
    fun `test decryptVote produces original vote`() {
        val voteChoice = BooleanVote.YES
        val encryptedVote = simpleHeService.encryptVote(voteChoice)

        // Decrypt the vote using the private key
        val decryptedVote = BooleanVote.fromBigInteger(simpleHeService.decryptVote(encryptedVote))

        // Check that the decrypted vote matches the original choice
        assertEquals("Decrypted vote should match original choice", voteChoice, decryptedVote)
    }

    @Test
    fun `test summation of encrypted votes`() {
        val voteChoices = listOf(BooleanVote.NO, BooleanVote.YES, BooleanVote.YES, BooleanVote.NO, BooleanVote.YES)

        println(voteChoices.map { it.toBigInteger() })

        val encryptedVotes = voteChoices.map { simpleHeService.encryptVote(it) }

        println(encryptedVotes.map { it.bitLength() })

        // Sum the encrypted votes
        val summedEncryptedVote = simpleHeService.sumEncryptedVotes(encryptedVotes)

        // Decrypt the summed vote
        val decryptedSum = simpleHeService.decryptVote(summedEncryptedVote)
        val realSum = voteChoices.map { it.toBigInteger() }.reduce { acc, bigInteger -> acc + bigInteger }

        // Check that the sum matches the original choices
        assertEquals("Decrypted sum should match original choices", realSum, decryptedSum)
    }

}
