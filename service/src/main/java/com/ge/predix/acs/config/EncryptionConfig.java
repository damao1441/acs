package com.ge.predix.acs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ge.predix.acs.encryption.Encryptor;

@Configuration
public class EncryptionConfig {

    @Bean
    public Encryptor encryptor() {
        return Encryptor.getInstance();
    }

}
