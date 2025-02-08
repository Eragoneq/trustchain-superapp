package nl.tudelft.trustchain.smartphonedemocracy.crypto.models

import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierPublicKey
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable
data class SerializablePaillierPublicKey(
    val n: String,
    val g: String,
    val mod: String,
    val keySize: Int
) {
    fun toPaillierPublicKey(): PaillierPublicKey {
        return PaillierPublicKey(
            keySize, BigInteger(n), BigInteger(mod), BigInteger(g),
        )
    }
}
