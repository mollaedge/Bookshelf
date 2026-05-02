package com.arturmolla.bookshelf;

import com.arturmolla.bookshelf.config.ConfigAuditorAware;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
/**
 * Provides the auditorAware bean required by @EnableJpaAuditing for @DataJpaTest slices.
 * The main ApiApplication still carries @EnableJpaAuditing(auditorAwareRef = "auditorAware");
 * this config supplies the matching bean so the JPA auditing infrastructure can find it.
 * Import this config via @Import(TestJpaAuditingConfig.class) in any repository test class.
 */
@TestConfiguration
public class TestJpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorAware() {
        return new ConfigAuditorAware();
    }
}

