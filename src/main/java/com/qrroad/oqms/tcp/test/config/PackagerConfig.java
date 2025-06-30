package com.qrroad.oqms.tcp.test.config;

import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Slf4j
@Configuration
public class PackagerConfig {

    @Bean
    public ISOPackager isoPackager() {
        try {
            log.info("Loading ISO8583 packager configuration");

            ClassPathResource resource = new ClassPathResource("config/iso8583-test.xml");
            try (InputStream inputStream = resource.getInputStream()) {
                GenericPackager packager = new GenericPackager();
                packager.readFile(inputStream);

                log.info("Successfully loaded ISO8583 packager");
                return packager;
            }

        } catch (Exception e) {
            log.error("Failed to load ISO8583 packager", e);
            throw new IllegalStateException("Failed to initialize ISO8583 packager", e);
        }
    }
}
