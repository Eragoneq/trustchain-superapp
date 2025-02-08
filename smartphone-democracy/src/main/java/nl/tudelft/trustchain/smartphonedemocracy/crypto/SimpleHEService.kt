package nl.tudelft.trustchain.smartphonedemocracy.crypto

import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierCipher
import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierKeyPairGenerator
import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierPrivateKey
import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierPublicKey
import nl.tudelft.trustchain.smartphonedemocracy.data.models.BooleanVote
import java.math.BigInteger
import java.security.KeyPair
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

typealias HEEncryptedValue = BigInteger

/**
 * Service for Homomorphic Encryption operations using the Paillier cryptosystem
 * Suitable only for simple voting applications, with Boolean type votes.
 */
@Singleton
class SimpleHEService @Inject constructor() {
    private val secureRandom = SecureRandom()
    private val keyPair by lazy {
        generateKeyPair()
    }

    val publicKey: PaillierPublicKey by lazy { keyPair.public as PaillierPublicKey }
    private val privateKey: PaillierPrivateKey by lazy { keyPair.private as PaillierPrivateKey }

    /**
     * Generates a new Paillier key pair
     * @param keySize The size of the key in bits (default: 2048)
     * @return A pair of (public key, private key)
     */
    private fun generateKeyPair(keySize: Int = 2048): KeyPair {
        return PaillierKeyPairGenerator().apply {
            initialize(keySize, secureRandom)
        }.generateKeyPair()
    }

    /**
     * Encrypts a vote using the Paillier cryptosystem
     * @param voteChoice The vote to encrypt (must be a positive integer)
     * @return The encrypted vote
     */
    fun encryptVote(voteChoice: BooleanVote): HEEncryptedValue {
        return PaillierCipher.encrypt(voteChoice.toBigInteger(), publicKey)
    }

    /**
     * Decrypts a vote using the Paillier cryptosystem
     * @param encryptedVote The encrypted vote to decrypt
     * @return The original vote choice as an Int
     */
    fun decryptVote(encryptedVote: HEEncryptedValue): BigInteger {
        val decryptedVote = PaillierCipher.decrypt(encryptedVote, privateKey)
        return decryptedVote
    }

    /**
     * Sums a list of encrypted votes using the Paillier cryptosystem
     * @param encryptedVotes The list of encrypted votes to sum
     * @return The sum of the encrypted votes as an encrypted value
     */
    fun sumEncryptedVotes(
        encryptedVotes: List<HEEncryptedValue>,
    ): HEEncryptedValue {
        if (encryptedVotes.isEmpty()) {
            throw IllegalArgumentException("List of encrypted votes cannot be empty")
        }

        // Start with the first encrypted vote
        var sum = encryptedVotes[0]

        // Sum the rest of the encrypted votes
        for (i in 1 until encryptedVotes.size) {
            sum = PaillierCipher.add(sum, encryptedVotes[i], publicKey)
        }

        return sum
    }
}
