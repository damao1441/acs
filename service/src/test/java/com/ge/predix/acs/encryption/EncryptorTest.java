package com.ge.predix.acs.encryption;

import org.junit.Assert;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.Test;

public class EncryptorTest {

    private static final String ENCRYPTION_KEY_VAR_NAME = "encryptionKey";
    private static final String VALUE_TO_ENCRYPT = "testValue";

    @Test
    public void testEncryptCompleteFlow() {
        Encryptor encryption = Encryptor.getInstance();
        ReflectionTestUtils.setField(encryption, ENCRYPTION_KEY_VAR_NAME, "FooBarFooBarFooB");
        Assert.assertEquals(encryption.decrypt(encryption.encrypt(VALUE_TO_ENCRYPT)), VALUE_TO_ENCRYPT);
    }

    @Test(expectedExceptions = { SymmetricKeyValidationException.class })
    public void testCreateEncryptionWithWrongKeySize() {
        Encryptor encryption = Encryptor.getInstance();
        ReflectionTestUtils.setField(encryption, ENCRYPTION_KEY_VAR_NAME, "Key_With_Wrong_Size");
        encryption.encrypt(VALUE_TO_ENCRYPT);
    }

}
