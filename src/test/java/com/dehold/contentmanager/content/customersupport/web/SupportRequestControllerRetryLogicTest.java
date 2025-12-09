package com.dehold.contentmanager.content.customersupport.web;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.service.SupportRequestService;
import com.dehold.contentmanager.content.customersupport.web.dto.CustomerRequestDto;
import com.dehold.contentmanager.content.customersupport.web.dto.PartialResultResponse;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SupportRequestController Retry Logic Integration Tests")
class SupportRequestControllerRetryLogicTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupportRequestService service;

    private SupportRequest createTestRequest(UUID id) {
        return new SupportRequest(
                id,
                UUID.randomUUID(),
                "Test request",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now()
        );
    }

    // ==================== GET /api/customer-requests Tests ====================

    @Test
    @DisplayName("GET /api/customer-requests should return successful results")
    void testGetAllSuccess() throws Exception {
        // Arrange
        List<SupportRequest> requests = List.of(
                createTestRequest(UUID.randomUUID()),
                createTestRequest(UUID.randomUUID())
        );
        
        PartialResultResponse<SupportRequest> response = new PartialResultResponse<>(
                requests,
                new HashMap<>()
        );
        response.setPartial(false);
        
        when(service.findAllWithRetry()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/customer-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successfulResults", hasSize(2)))
                .andExpect(jsonPath("$.partial", is(false)))
                .andExpect(jsonPath("$.successCount", is(2)))
                .andExpect(jsonPath("$.failureCount", is(0)));

        verify(service, times(1)).findAllWithRetry();
    }

    @Test
    @DisplayName("GET /api/customer-requests should return partial results on failure")
    void testGetAllPartialResults() throws Exception {
        // Arrange
        SupportRequest successRequest = createTestRequest(UUID.randomUUID());
        UUID failedId = UUID.randomUUID();
        
        Map<UUID, PartialResultResponse.FailedIdInfo> failedIds = new HashMap<>();
        failedIds.put(failedId, new PartialResultResponse.FailedIdInfo(
                failedId, "Database timeout", 0
        ));
        
        PartialResultResponse<SupportRequest> response = new PartialResultResponse<>(
                List.of(successRequest),
                failedIds
        );
        response.setPartial(true);
        
        when(service.findAllWithRetry()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/customer-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successfulResults", hasSize(1)))
                .andExpect(jsonPath("$.failedIds", aMapWithSize(1)))
                .andExpect(jsonPath("$.partial", is(true)))
                .andExpect(jsonPath("$.successCount", is(1)))
                .andExpect(jsonPath("$.failureCount", is(1)));

        verify(service, times(1)).findAllWithRetry();
    }

    @Test
    @DisplayName("GET /api/customer-requests should include retry info")
    void testGetAllRetryInfo() throws Exception {
        // Arrange
        List<SupportRequest> requests = Collections.emptyList();
        PartialResultResponse.RetryInfo retryInfo = 
                new PartialResultResponse.RetryInfo(1000, "Exponential backoff", 4, 2);
        
        PartialResultResponse<SupportRequest> response = new PartialResultResponse<>(
                requests,
                new HashMap<>(),
                retryInfo
        );
        response.setPartial(false);
        
        when(service.findAllWithRetry()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/customer-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retryInfo.retryAfterMs", is(1000)))
                .andExpect(jsonPath("$.retryInfo.retryStrategy", containsString("Exponential")))
                .andExpect(jsonPath("$.retryInfo.maxRetries", is(4)))
                .andExpect(jsonPath("$.retryInfo.currentAttempt", is(2)));

        verify(service, times(1)).findAllWithRetry();
    }

    @Test
    @DisplayName("GET /api/customer-requests should include timestamp")
    void testGetAllTimestamp() throws Exception {
        // Arrange
        PartialResultResponse<SupportRequest> response = new PartialResultResponse<>(
                new ArrayList<>(),
                new HashMap<>()
        );
        response.setPartial(false);
        
        when(service.findAllWithRetry()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/customer-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(service, times(1)).findAllWithRetry();
    }

    // ==================== GET /api/customer-requests/{id} Tests ====================

    @Test
    @DisplayName("GET /api/customer-requests/{id} should return successful result with 200 status")
    void testGetByIdSuccess() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        SupportRequest request = createTestRequest(id);
        
        PartialResultResponse<SupportRequest> response = new PartialResultResponse<>(
                List.of(request),
                new HashMap<>()
        );
        response.setPartial(false);
        
        when(service.findByIdWithRetry(id)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/customer-requests/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successfulResults", hasSize(1)))
                .andExpect(jsonPath("$.successfulResults[0].id", is(id.toString())))
                .andExpect(jsonPath("$.partial", is(false)))
                .andExpect(jsonPath("$.successCount", is(1)))
                .andExpect(jsonPath("$.failureCount", is(0)));

        verify(service, times(1)).findByIdWithRetry(id);
    }

    @Test
    @DisplayName("GET /api/customer-requests/{id} should return partial result with 206 status on failure")
    void testGetByIdPartialResult() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        
        Map<UUID, PartialResultResponse.FailedIdInfo> failedIds = new HashMap<>();
        failedIds.put(id, new PartialResultResponse.FailedIdInfo(
                id, "Database connection failed after 4 retry attempts", 0
        ));
        
        PartialResultResponse<SupportRequest> response = new PartialResultResponse<>(
                new ArrayList<>(),
                failedIds
        );
        response.setPartial(true);
        
        when(service.findByIdWithRetry(id)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/customer-requests/" + id))
                .andExpect(status().isPartialContent()) // 206
                .andExpect(jsonPath("$.successfulResults", hasSize(0)))
                .andExpect(jsonPath("$.failedIds", aMapWithSize(1)))
                .andExpect(jsonPath("$.partial", is(true)))
                .andExpect(jsonPath("$.failureCount", is(1)));

        verify(service, times(1)).findByIdWithRetry(id);
    }

    @Test
    @DisplayName("GET /api/customer-requests/{id} should include failure details")
    void testGetByIdFailureDetails() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        String failureReason = "Network timeout after retries";
        
        Map<UUID, PartialResultResponse.FailedIdInfo> failedIds = new HashMap<>();
        PartialResultResponse.FailedIdInfo failedInfo = new PartialResultResponse.FailedIdInfo(
                id, failureReason, 0
        );
        failedIds.put(id, failedInfo);
        
        PartialResultResponse<SupportRequest> response = new PartialResultResponse<>(
                new ArrayList<>(),
                failedIds
        );
        response.setPartial(true);
        
        when(service.findByIdWithRetry(id)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/customer-requests/" + id))
                .andExpect(status().isPartialContent())
                .andExpect(jsonPath("$.failedIds['" + id + "'].id", is(id.toString())))
                .andExpect(jsonPath("$.failedIds['" + id + "'].failureReason", containsString("timeout")))
                .andExpect(jsonPath("$.failedIds['" + id + "'].remainingRetries", is(0)));

        verify(service, times(1)).findByIdWithRetry(id);
    }

    @Test
    @DisplayName("GET /api/customer-requests/{id} should return retry info")
    void testGetByIdRetryInfo() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        SupportRequest request = createTestRequest(id);
        
        PartialResultResponse.RetryInfo retryInfo = 
                new PartialResultResponse.RetryInfo(2000, "Exponential backoff", 4, 3);
        
        PartialResultResponse<SupportRequest> response = new PartialResultResponse<>(
                List.of(request),
                new HashMap<>(),
                retryInfo
        );
        response.setPartial(false);
        
        when(service.findByIdWithRetry(id)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/customer-requests/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retryInfo.retryAfterMs", is(2000)))
                .andExpect(jsonPath("$.retryInfo.currentAttempt", is(3)))
                .andExpect(jsonPath("$.retryInfo.maxRetries", is(4)));

        verify(service, times(1)).findByIdWithRetry(id);
    }

    @Test
    @DisplayName("GET /api/customer-requests/{id} should handle entity not found")
    void testGetByIdEntityNotFound() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        
        when(service.findByIdWithRetry(id))
                .thenThrow(EntityNotFoundException.of("CustomerRequest", id.toString()));

        // Act & Assert
        mockMvc.perform(get("/api/customer-requests/" + id))
                .andExpect(status().isNotFound());

        verify(service, times(1)).findByIdWithRetry(id);
    }

    @Test
    @DisplayName("GET /api/customer-requests/{id} should handle service unavailable")
    void testGetByIdServiceUnavailable() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        
        when(service.findByIdWithRetry(id))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "Service temporarily unavailable"
                ));

        // Act & Assert
        mockMvc.perform(get("/api/customer-requests/" + id))
                .andExpect(status().isServiceUnavailable());

        verify(service, times(1)).findByIdWithRetry(id);
    }

    @Test
    @DisplayName("GET /api/customer-requests/{id} should handle invalid UUID format")
    void testGetByIdInvalidUUID() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/customer-requests/invalid-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // ==================== Response Content Type Tests ====================

    @Test
    @DisplayName("GET /api/customer-requests should return JSON content type")
    void testGetAllContentType() throws Exception {
        // Arrange
        PartialResultResponse<SupportRequest> response = new PartialResultResponse<>(
                new ArrayList<>(),
                new HashMap<>()
        );
        response.setPartial(false);
        
        when(service.findAllWithRetry()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/customer-requests"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/customer-requests/{id} should return JSON content type")
    void testGetByIdContentType() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        PartialResultResponse<SupportRequest> response = new PartialResultResponse<>(
                List.of(createTestRequest(id)),
                new HashMap<>()
        );
        response.setPartial(false);
        
        when(service.findByIdWithRetry(id)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/customer-requests/" + id))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
