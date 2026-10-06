package com.np3.ledgerai.infrastructure.security;

import com.np3.ledgerai.RepositoryRoot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the Keycloak realm shipped for local development consistent with what the code expects:
 * every role checked by @PreAuthorize exists, the frontend uses PKCE, and docker-compose mounts a real file.
 */
class KeycloakRealmFileTest {

    private static final Path ROOT = RepositoryRoot.path();
    private static final Path REALM_FILE = ROOT.resolve("keycloak/ledgerai-realm.json");
    private static JsonNode realm;

    @BeforeAll
    static void loadRealm() throws Exception {
        realm = JsonMapper.builder().build().readTree(Files.readString(REALM_FILE));
    }

    @Test
    void dockerComposeMountsARealmFileThatExists() throws Exception {
        String compose = Files.readString(ROOT.resolve("docker-compose.yml"));
        Matcher matcher = Pattern.compile("\\./(\\S+\\.json):/opt/keycloak/data/import/").matcher(compose);

        assertThat(matcher.find()).as("compose mounts a realm file").isTrue();
        assertThat(ROOT.resolve(matcher.group(1))).exists();
    }

    @Test
    void definesEveryRoleTheApplicationChecks() {
        assertThat(names(realm.get("roles").get("realm"), "name")).contains("viewer", "accountant", "admin");
    }

    @Test
    void frontendClientUsesAuthorizationCodeWithPkceAndNoPasswordGrant() {
        JsonNode frontend = client("ledgerai-frontend");

        assertThat(frontend.get("publicClient").asBoolean()).isTrue();
        assertThat(frontend.get("standardFlowEnabled").asBoolean()).isTrue();
        assertThat(frontend.get("directAccessGrantsEnabled").asBoolean()).isFalse();
        assertThat(frontend.get("attributes").get("pkce.code.challenge.method").asString()).isEqualTo("S256");
    }

    @Test
    void localUsersCoverEachRole() {
        assertThat(names(realm.get("users"), "username")).containsExactlyInAnyOrder("viewer", "accountant", "admin");
    }

    private static JsonNode client(String clientId) {
        for (JsonNode client : realm.get("clients")) {
            if (clientId.equals(client.get("clientId").asString())) {
                return client;
            }
        }
        throw new AssertionError("client not found: " + clientId);
    }

    private static List<String> names(JsonNode array, String field) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.get(field).asString()));
        return values;
    }
}
