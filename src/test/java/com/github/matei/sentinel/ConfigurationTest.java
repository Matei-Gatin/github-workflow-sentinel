package com.github.matei.sentinel;

import com.github.matei.sentinel.config.Configuration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {

    @Test
    void testValidConfiguration() {
        Configuration config = new Configuration("owner/repo", "ghp_token123");

        assertEquals("owner/repo", config.getRepository());
        assertEquals("ghp_token123", config.getToken());
        assertEquals("owner", config.getOwner());
        assertEquals("repo", config.getRepo());
    }

    @Test
    void testInvalidRepository_NoSlash() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Configuration("ownerrepo", "ghp_token123");
        });
    }

    @Test
    void testInvalidRepository_MultipleSlashes() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Configuration("owner/repo/extra", "ghp_token123");
        });
    }

    @Test
    void testInvalidRepository_Null() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Configuration(null, "ghp_token123");
        });
    }

    @Test
    void testConfigurationWithComplexNames() {
        Configuration config = new Configuration("my-org/my-repo-name", "token");

        assertEquals("my-org", config.getOwner());
        assertEquals("my-repo-name", config.getRepo());
    }
}
