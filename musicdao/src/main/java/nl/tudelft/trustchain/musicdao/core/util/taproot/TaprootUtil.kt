package nl.tudelft.trustchain.musicdao.core.util.taproot

import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger
import kotlin.experimental.and

/**
 * Ported from python code located here: https://github.com/bitcoinops/taproot-workshop/blob/master/test_framework/key.py
 */
class TaprootUtil {
    companion object {
        /**
         * Construct a {@link org.bitcoinj.core.SegwitAddress} from a byte array.
         *
         * @param program program
         * @return
         */
        fun keyToWitness(pubKeyData: ByteArray): String {
            val program = byteArrayOf(pubKeyData[0] and 1.toByte()).plus(pubKeyData.drop(1))

            assert(2 <= program.size && program.size <= 40)

            return SegwitAddress(NetworkParameters.fromPmtProtocolID("regtest"), 0x01, program).toString()
        }

        /**
         * Generate a random valid bip-schnorr nonce.
         *
         * See https://github.com/bitcoinops/bips/blob/v0.1/bip-schnorr.mediawiki#Signing.
         * This implementation ensures the y-coordinate of the nonce point is a quadratic residue modulo the field size.
         */
        fun generateSchnorrNonce(privateKey: ByteArray): Pair<ECKey, ECPoint> {
            val nonceKey = ECKey.fromPrivate(privateKey)
            var r = nonceKey.pubKeyPoint
            r = r.normalize()

            if (Schnorr.jacobi(r.affineYCoord.toBigInteger()) != BigInteger.ONE) {
                r = r.negate()
            }

            return Pair(nonceKey, r)
        }
    }
}
