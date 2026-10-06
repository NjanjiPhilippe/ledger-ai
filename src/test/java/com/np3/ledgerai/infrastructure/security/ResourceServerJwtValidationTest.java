package com.np3.ledgerai.infrastructure.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validates real signed JWTs through the real security filter chain, against an OIDC server that looks like
 * Keycloak (discovery + a JWKS with a signing key AND an RSA-OAEP encryption key).
 *
 * Why it exists: the application once started fine yet rejected every Keycloak token, because an
 * authorization-server starter on the classpath silently provided a JwtDecoder backed by a random in-memory key.
 * Nothing but a test that actually validates a token can catch that.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ResourceServerJwtValidationTest {

    private static final String ACCOUNT_BODY = "{\"name\":\"Cash\",\"type\":\"ASSET\",\"currencyCode\":\"XAF\"}";

    private static final FakeOidcServer OIDC = FakeOidcServer.start();

    @DynamicPropertySource
    static void issuer(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", OIDC::issuer);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aTokenSignedByTheIssuerIsAccepted() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + OIDC.token("viewer")))
                .andExpect(status().isOk());
    }

    @Test
    void aViewerCannotCreateAnAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts").header("Authorization", "Bearer " + OIDC.token("viewer"))
                        .contentType(MediaType.APPLICATION_JSON).content(ACCOUNT_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAccountantCanCreateAnAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts").header("Authorization", "Bearer " + OIDC.token("accountant"))
                        .contentType(MediaType.APPLICATION_JSON).content(ACCOUNT_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void aTokenSignedByAnUnknownKeyIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + OIDC.tokenSignedByAnotherKey("viewer")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aRequestWithoutATokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    /** A tiny Keycloak look-alike: OIDC discovery and a JWKS on a random local port. */
    private static final class FakeOidcServer {
        private final RSAKey signingKey;
        private final RSAKey strangerKey;
        private final String issuer;

        private FakeOidcServer(RSAKey signingKey, RSAKey strangerKey, String issuer) {
            this.signingKey = signingKey;
            this.strangerKey = strangerKey;
            this.issuer = issuer;
        }

        static FakeOidcServer start() {
            try {
                RSAKey signing = new RSAKeyGenerator(2048).keyID("sig-key").keyUse(KeyUse.SIGNATURE)
                        .algorithm(JWSAlgorithm.RS256).generate();
                RSAKey encryption = new RSAKeyGenerator(2048).keyID("enc-key").keyUse(KeyUse.ENCRYPTION)
                        .algorithm(com.nimbusds.jose.JWEAlgorithm.RSA_OAEP).generate();
                RSAKey stranger = new RSAKeyGenerator(2048).keyID("sig-key").keyUse(KeyUse.SIGNATURE)
                        .algorithm(JWSAlgorithm.RS256).generate(); // same kid, different key material
                String jwks = new JWKSet(List.of(signing.toPublicJWK(), encryption.toPublicJWK())).toString();

                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                String issuer = "http://127.0.0.1:" + server.getAddress().getPort() + "/realms/test";
                String discovery = "{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"" + issuer
                        + "/protocol/openid-connect/certs\",\"id_token_signing_alg_values_supported\":[\"RS256\"]}";
                server.createContext("/realms/test/.well-known/openid-configuration", ex -> respond(ex, discovery));
                server.createContext("/realms/test/protocol/openid-connect/certs", ex -> respond(ex, jwks));
                server.setExecutor(null);
                server.start();
                return new FakeOidcServer(signing, stranger, issuer);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot start the fake OIDC server", e);
            }
        }

        private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws java.io.IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }

        String issuer() {
            return issuer;
        }

        String token(String role) {
            return sign(signingKey, role);
        }

        String tokenSignedByAnotherKey(String role) {
            return sign(strangerKey, role);
        }

        private String sign(RSAKey key, String role) {
            try {
                JWTClaimsSet claims = new JWTClaimsSet.Builder()
                        .issuer(issuer)
                        .subject(UUID.randomUUID().toString())
                        .expirationTime(new Date(System.currentTimeMillis() + 3_600_000))
                        .claim("realm_access", Map.of("roles", List.of(role)))
                        .build();
                SignedJWT jwt = new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
                jwt.sign(new RSASSASigner(key));
                return jwt.serialize();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
