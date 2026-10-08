package com.np3.ledgerai.web.controller;

import com.np3.ledgerai.domain.port.CurrentUserProvider;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeController.class)
@AutoConfigureMockMvc(addFilters = false)
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CurrentUserProvider currentUserProvider;
    @MockitoBean
    private TenantContext tenantContext;

    @Test
    void meReturnsPlainIdentifiersNotWrappedValueObjects() throws Exception {
        UUID user = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(UserId.of(user));
        when(tenantContext.currentTenantId()).thenReturn(TenantId.of(tenant));

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.toString()))
                .andExpect(jsonPath("$.tenantId").value(tenant.toString()));
    }
}
