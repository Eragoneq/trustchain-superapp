package nl.tudelft.trustchain.smartphonedemocracy.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages cryptographic keys and secrets using Android's Keystore system
 */
@Singleton
class KeyStoreManager @Inject constructor() {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH = 128
        private const val KEY_SIZE = 256
    }

    /**
     * Stores a secret byte array in the Android Keystore
     * @param alias The alias to store the secret under
     * @param secret The secret to store
     */
    fun storeSecret(alias: String, secret: ByteArray) {
        // Generate a new AES key for this secret
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        val secretKey = keyGenerator.generateKey()

        // Encrypt the secret
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedSecret = cipher.doFinal(secret)

        // Store the encrypted secret and IV
        keyStore.setEntry(
            "${alias}_data",
            KeyStore.SecretKeyEntry(secretKey),
            null
        )
//        keyStore.setEntry(
//            "${alias}_iv",
//            KeyStore.SecretKeyEntry(SecretKey(iv, "RAW")),
//            null
//        )
//        keyStore.setEntry(
//            "${alias}_encrypted",
//            KeyStore.SecretKeyEntry(SecretKey(encryptedSecret, "RAW")),
//            null
//        )
    }

    /**
     * Retrieves a stored secret from the Android Keystore
     * @param alias The alias the secret was stored under
     * @return The decrypted secret
     */
    fun retrieveSecret(alias: String): ByteArray {
        val secretKey = keyStore.getKey("${alias}_data", null) as SecretKey
        val iv = (keyStore.getKey("${alias}_iv", null) as SecretKey).encoded
        val encryptedSecret = (keyStore.getKey("${alias}_encrypted", null) as SecretKey).encoded

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, iv))
        return cipher.doFinal(encryptedSecret)
    }

    /**
     * Generates and stores a new key pair in the Android Keystore
     * @param alias The alias to store the key pair under
     * @return The generated KeyPair
     */
    fun generateAndStoreKeyPair(alias: String): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()

        keyPairGenerator.initialize(keyGenParameterSpec)
        val keyPair = keyPairGenerator.generateKeyPair()

        return keyPair
    }

    /**
     * Retrieves a stored key pair from the Android Keystore
     * @param alias The alias the key pair was stored under
     * @return The KeyPair
     */
    fun retrieveKeyPair(alias: String): KeyPair {
        val privateKey = keyStore.getKey(alias, null) as PrivateKey
        val publicKey = keyStore.getCertificate(alias).publicKey
        return KeyPair(publicKey, privateKey)
    }

    /**
     * Checks if a key or secret exists in the Android Keystore
     * @param alias The alias to check
     * @return true if the key/secret exists, false otherwise
     */
    fun containsAlias(alias: String): Boolean {
        return keyStore.containsAlias(alias)
    }

    /**
     * Deletes a key or secret from the Android Keystore
     * @param alias The alias to delete
     */
    fun deleteAlias(alias: String) {
        keyStore.deleteEntry(alias)
    }
}
