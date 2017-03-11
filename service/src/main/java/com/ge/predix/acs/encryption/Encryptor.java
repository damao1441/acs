package com.ge.predix.acs.encryption;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.primitives.Bytes;

// CHECKSTYLE:OFF: FinalClass
public class Encryptor {

    @Value("${ENCRYPTION_KEY}")
    private String encryptionKey;

    private static final Encryptor INSTANCE = new Encryptor();
    private static final Logger LOGGER = LoggerFactory.getLogger(Encryptor.class);

    private static final String ALGO_WITH_PADDING = "AES/CBC/PKCS5PADDING";
    private static final String ALGO = "AES";
    private static final String ENCODING = "UTF-8";
    private static final int IV_LENGTH_IN_BYTES = 16;
    private static final int KEY_LENGTH_IN_BYTES = 16;

    private Cipher cipher;

    private Encryptor() {
        try {
            this.cipher = Cipher.getInstance(ALGO_WITH_PADDING);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            LOGGER.error("Can not created instance of cipher with algorithm" + ALGO_WITH_PADDING);
        }
    }

    public static Encryptor getInstance() {
        return INSTANCE;
    }

    public String encrypt(final String value) {
        validateKeyLength();
        try {
            byte[] ivBytes = new byte[IV_LENGTH_IN_BYTES];
            SecureRandom.getInstanceStrong().nextBytes(ivBytes);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            SecretKeySpec skeySpec = new SecretKeySpec(this.encryptionKey.getBytes(ENCODING), ALGO);

            this.cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = this.cipher.doFinal(value.getBytes());
            byte[] result = Bytes.concat(ivBytes, encrypted);

            return Base64.encodeBase64String(result);
        } catch (Throwable ex) {
            LOGGER.error("Unable to encrypt");
            throw new EncryptionFailureException(ex);
        }
    }

    public String decrypt(final String encrypted) {
        validateKeyLength();
        try {
            byte[] encryptedBytes = Base64.decodeBase64(encrypted);
            byte[] ivBytes = Arrays.copyOfRange(encryptedBytes, 0, IV_LENGTH_IN_BYTES);
            byte[] encryptedSecretBytes = Arrays.copyOfRange(encryptedBytes, IV_LENGTH_IN_BYTES, encryptedBytes.length);

            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            SecretKeySpec skeySpec = new SecretKeySpec(this.encryptionKey.getBytes(ENCODING), ALGO);

            this.cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] original = this.cipher.doFinal(encryptedSecretBytes);

            return new String(original);
        } catch (Throwable ex) {
            LOGGER.error("Unable to decrypt");
            throw new DecryptionFailureException(ex);
        }
    }

    private void validateKeyLength() {
        if (this.encryptionKey.length() != KEY_LENGTH_IN_BYTES) {
            throw new SymmetricKeyValidationException("Encryption key must be string of length " + KEY_LENGTH_IN_BYTES);
        }
    }
}
// CHECKSTYLE:ON: FinalClass
