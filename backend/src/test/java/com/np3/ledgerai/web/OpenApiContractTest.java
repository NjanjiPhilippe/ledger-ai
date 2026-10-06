package com.np3.ledgerai.web;

import com.np3.ledgerai.RepositoryRoot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The API contract is versioned in docs/api/openapi.json and the frontend generates its TypeScript types from it.
 * This test fails when the API no longer matches the committed file, so the contract cannot drift silently.
 *
 * After an intentional API change, regenerate the file:
 *   ./mvnw test -Dtest=OpenApiContractTest -Dopenapi.write=true
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractTest {

    private static final Path CONTRACT = RepositoryRoot.path().resolve("docs/api/openapi.json");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void theCommittedContractMatchesTheApi() throws Exception {
        String generated = canonicalApiDocs();

        if (Boolean.getBoolean("openapi.write")) {
            Files.createDirectories(CONTRACT.getParent());
            Files.writeString(CONTRACT, generated);
            return;
        }

        assertThat(CONTRACT)
                .as("docs/api/openapi.json must exist: run ./mvnw test -Dtest=OpenApiContractTest -Dopenapi.write=true")
                .exists();
        assertThat(Files.readString(CONTRACT).replace("\r\n", "\n"))
                .as("The API changed: regenerate docs/api/openapi.json with "
                        + "./mvnw test -Dtest=OpenApiContractTest -Dopenapi.write=true, and commit it")
                .isEqualTo(generated);
    }

    @Test
    void amountsAreDeclaredAsStringsInTheContract() throws Exception {
        String api = canonicalApiDocs();

        assertThat(api).contains("\"Exact decimal amount");
        assertThat(objectMapper.readTree(api).at("/components/schemas/BalanceResponse/properties/amount/type")
                .asString()).isEqualTo("string");
    }

    /** Stable text: keys sorted, volatile parts removed, so the file only changes when the API does. */
    private String canonicalApiDocs() throws Exception {
        String raw = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> document = (Map<String, Object>) sorted(objectMapper.readValue(raw, Object.class));
        document.remove("servers"); // depends on the host the document was requested from
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document) + "\n";
    }

    @SuppressWarnings("unchecked")
    private static Object sorted(Object node) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> result = new TreeMap<>();
            ((Map<String, Object>) map).forEach((key, value) -> result.put(key, sorted(value)));
            return result;
        }
        if (node instanceof List<?> list) {
            return list.stream().map(OpenApiContractTest::sorted).toList();
        }
        return node;
    }
}
