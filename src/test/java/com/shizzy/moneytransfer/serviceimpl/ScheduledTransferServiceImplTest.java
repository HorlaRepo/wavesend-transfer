package com.shizzy.moneytransfer.serviceimpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.ScheduledTransferInitiationResponse;
import com.shizzy.moneytransfer.dto.ScheduledTransferNotification;
import com.shizzy.moneytransfer.dto.ScheduledTransferRequestDTO;
import com.shizzy.moneytransfer.dto.ScheduledTransferResponseDTO;
import com.shizzy.moneytransfer.dto.ScheduledTransferVerificationRequest;
import com.shizzy.moneytransfer.dto.TransactionNotification;
import com.shizzy.moneytransfer.enums.RecurrenceType;
import com.shizzy.moneytransfer.enums.ScheduleStatus;
import com.shizzy.moneytransfer.exception.InvalidOtpException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.kafka.NotificationProducer;
import com.shizzy.moneytransfer.model.ScheduledTransfer;
import com.shizzy.moneytransfer.repository.ScheduledTransferRepository;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.service.MoneyTransferService;
import com.shizzy.moneytransfer.service.OtpService;

@ExtendWith(MockitoExtension.class)
class ScheduledTransferServiceImplTest {

        @Mock
        private ScheduledTransferRepository scheduledTransferRepository;

        @Mock
        private MoneyTransferService moneyTransferService;

        @Mock
        private KeycloakService keycloakService;

        @Mock
        private NotificationProducer notificationProducer;

        @Mock
        private OtpService otpService;

        @Mock
        private CacheManager cacheManager;

        @Mock
        private Cache cache;

        @InjectMocks
        private ScheduledTransferServiceImpl scheduledTransferService;

        private String userId;
        private ScheduledTransferRequestDTO requestDTO;
        private ScheduledTransfer scheduledTransfer;

        @BeforeEach
        void setUp() {
                userId = "user123";

                requestDTO = new ScheduledTransferRequestDTO(
                                "sender@example.com",
                                "receiver@example.com",
                                BigDecimal.valueOf(100.00),
                                "Test transfer",
                                LocalDateTime.now().plusDays(1),
                                RecurrenceType.NONE,
                                null,
                                null);

                scheduledTransfer = ScheduledTransfer.builder()
                                .id(1L)
                                .senderEmail("sender@example.com")
                                .receiverEmail("receiver@example.com")
                                .createdBy(userId)
                                .amount(BigDecimal.valueOf(100.00))
                                .description("Test transfer")
                                .scheduledDateTime(LocalDateTime.now().plusDays(1))
                                .status(ScheduleStatus.PENDING)
                                .recurrenceType(RecurrenceType.NONE)
                                .currentOccurrence(1)
                                .processed(false)
                                .retryCount(0)
                                .build();
        }

        @Test
        void scheduleTransfer_Successful() {
                when(keycloakService.existsUserByEmail(anyString())).thenReturn(
                                ApiResponse.<org.keycloak.representations.idm.UserRepresentation>builder()
                                                .success(true)
                                                .data(new org.keycloak.representations.idm.UserRepresentation())
                                                .build());
                when(scheduledTransferRepository.save(any(ScheduledTransfer.class))).thenReturn(scheduledTransfer);

                ApiResponse<ScheduledTransferResponseDTO> response = scheduledTransferService.scheduleTransfer(
                                requestDTO,
                                userId);

                assertTrue(response.isSuccess());
                assertEquals("Transfer scheduled successfully", response.getMessage());

                // Verify repository save was called once
                verify(scheduledTransferRepository, times(1)).save(any(ScheduledTransfer.class));

                // Verify notification was sent with specific topic and matching notification
                // object
                verify(notificationProducer, times(1)).sendNotification(
                                eq("scheduled-transfer-notifications"),
                                any(ScheduledTransferNotification.class));
        }

        @Test
        void scheduleTransfer_PastDate_ThrowsException() {
                requestDTO = new ScheduledTransferRequestDTO(
                                "sender@example.com",
                                "receiver@example.com",
                                BigDecimal.valueOf(100.00),
                                "Test transfer",
                                LocalDateTime.now().minusDays(1),
                                RecurrenceType.NONE,
                                null,
                                null);

                assertThrows(IllegalArgumentException.class,
                                () -> scheduledTransferService.scheduleTransfer(requestDTO, userId));
        }

        @Test
        void getUserScheduledTransfers_Paginated_Successful() {
                Pageable pageable = PageRequest.of(0, 10);
                Page<ScheduledTransfer> page = new PageImpl<>(Collections.singletonList(scheduledTransfer));
                when(scheduledTransferRepository.findByCreatedByOrderByScheduledDateTimeDesc(userId, pageable))
                                .thenReturn(page);

                ApiResponse<Page<ScheduledTransferResponseDTO>> response = scheduledTransferService
                                .getUserScheduledTransfers(userId, pageable);

                assertTrue(response.isSuccess());
                assertEquals(1, response.getData().getContent().size());
                verify(scheduledTransferRepository, times(1))
                                .findByCreatedByOrderByScheduledDateTimeDesc(userId, pageable);
        }

        @Test
        void cancelScheduledTransfer_Successful() {
                when(scheduledTransferRepository.findById(1L)).thenReturn(Optional.of(scheduledTransfer));
                when(scheduledTransferRepository.save(any(ScheduledTransfer.class))).thenReturn(scheduledTransfer);

                ApiResponse<ScheduledTransferResponseDTO> response = scheduledTransferService.cancelScheduledTransfer(
                                1L,
                                userId);

                assertTrue(response.isSuccess());
                assertEquals("Scheduled transfer cancelled successfully", response.getMessage());
                verify(scheduledTransferRepository, times(1)).save(any(ScheduledTransfer.class));

                
                // ScheduledTransferNotification
                verify(notificationProducer, times(1)).sendNotification(
                                eq("scheduled-transfer-notifications"),
                                any(ScheduledTransferNotification.class));
        }

        @Test
        void cancelScheduledTransfer_WrongUser_ThrowsException() {
                scheduledTransfer.setCreatedBy("otherUser");
                when(scheduledTransferRepository.findById(1L)).thenReturn(Optional.of(scheduledTransfer));

                assertThrows(IllegalArgumentException.class,
                                () -> scheduledTransferService.cancelScheduledTransfer(1L, userId));
        }

        @Test
        void initiateScheduledTransfer_Successful() {
                when(keycloakService.getUserById(userId)).thenReturn(
                                ApiResponse.<org.keycloak.representations.idm.UserRepresentation>builder()
                                                .success(true)
                                                .data(new org.keycloak.representations.idm.UserRepresentation() {
                                                        {
                                                                setEmail("sender@example.com");
                                                                setFirstName("John");
                                                        }
                                                })
                                                .build());
                when(cacheManager.getCache(anyString())).thenReturn(cache);

                ApiResponse<ScheduledTransferInitiationResponse> response = scheduledTransferService
                                .initiateScheduledTransfer(requestDTO, userId);

                assertTrue(response.isSuccess());
                assertNotNull(response.getData().getScheduledTransferToken());
                verify(otpService, times(1)).sendOtp(anyString(), anyString(), anyString(), any());
        }

        @Test
        void verifyAndScheduleTransfer_Successful() {
                String token = UUID.randomUUID().toString();
                ScheduledTransferVerificationRequest verificationRequest = new ScheduledTransferVerificationRequest();
                verificationRequest.setScheduledTransferToken(token);
                verificationRequest.setOtp("123456");

                when(keycloakService.getUserById(userId)).thenReturn(
                                ApiResponse.<org.keycloak.representations.idm.UserRepresentation>builder()
                                                .success(true)
                                                .data(new org.keycloak.representations.idm.UserRepresentation() {
                                                        {
                                                                setEmail("sender@example.com");
                                                        }
                                                })
                                                .build());
                when(otpService.verifyOtp(anyString(), anyString(), anyString())).thenReturn(new java.util.HashMap<>());
                when(cacheManager.getCache(anyString())).thenReturn(cache);
                when(cache.get(token)).thenReturn(
                                new Cache.ValueWrapper() {
                                        @Override
                                        public Object get() {
                                                return new ScheduledTransferServiceImpl.PendingScheduledTransfer(
                                                                requestDTO, userId);
                                        }
                                });
                when(scheduledTransferRepository.save(any(ScheduledTransfer.class))).thenReturn(scheduledTransfer);

                ApiResponse<ScheduledTransferResponseDTO> response = scheduledTransferService
                                .verifyAndScheduleTransfer(verificationRequest, userId);

                assertTrue(response.isSuccess());
                verify(cache, times(1)).evict(token);
                verify(scheduledTransferRepository, times(1)).save(any(ScheduledTransfer.class));
        }

        @Test
        void verifyAndScheduleTransfer_InvalidOtp_ThrowsException() {
                String token = UUID.randomUUID().toString();
                ScheduledTransferVerificationRequest verificationRequest = new ScheduledTransferVerificationRequest();
                verificationRequest.setScheduledTransferToken(token);
                verificationRequest.setOtp("123456");

                when(keycloakService.getUserById(userId)).thenReturn(
                                ApiResponse.<org.keycloak.representations.idm.UserRepresentation>builder()
                                                .success(true)
                                                .data(new org.keycloak.representations.idm.UserRepresentation() {
                                                        {
                                                                setEmail("sender@example.com");
                                                        }
                                                })
                                                .build());
                when(otpService.verifyOtp(anyString(), anyString(), anyString())).thenReturn(null);

                assertThrows(InvalidOtpException.class,
                                () -> scheduledTransferService.verifyAndScheduleTransfer(verificationRequest, userId));
        }
}