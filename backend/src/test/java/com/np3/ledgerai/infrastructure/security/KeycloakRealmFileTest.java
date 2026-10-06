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
    void frontendClientMayReturnToItsOwnPagesAfterLogout() {
        // "+" means: the valid redirect URIs. Without it Keycloak refuses the post_logout_redirect_uri of the frontend.
        assertThat(client("ledgerai-frontend").get("attributes").get("post.logout.redirect.uris").asString())
                .isEqualTo("+");
    }

    @Test
    void noClientForcesTheUnstyledBaseLoginTheme() {
        // "base" is only the parent of the real themes: forced on a client it gives a login page with no style at all.
        for (JsonNode client : realm.get("clients")) {
            JsonNode theme = client.get("attributes").get("login_theme");
            assertThat(theme == null ? "" : theme.asString())
                    .as("login theme of client " + client.get("clientId").asString())
                    .isNotEqualTo("base");
        }
    }

    @Test
    void loginPageIsBrandedAndOfferedInEnglishAndFrench() {
        assertThat(realm.get("displayName").asString()).isEqualTo("LedgerAI");
        assertThat(realm.get("internationalizationEnabled").asBoolean()).isTrue();
        assertThat(names(realm.get("supportedLocales"))).containsExactly("en", "fr");
        assertThat(realm.get("defaultLocale").asString()).isEqualTo("en");
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

    private static List<String> names(JsonNode strings) {
        List<String> values = new ArrayList<>();
        strings.forEach(node -> values.add(node.asString()));
        return values;
    }

    private static List<String> names(JsonNode array, String field) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.get(field).asString()));
        return values;
    }
}
