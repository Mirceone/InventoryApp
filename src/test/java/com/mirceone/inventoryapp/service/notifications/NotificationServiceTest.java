package com.mirceone.inventoryapp.service.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmMemberEntity;
import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.FirmStatusChangeSource;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.model.NotificationEntity;
import com.mirceone.inventoryapp.model.NotificationType;
import com.mirceone.inventoryapp.model.ProviderType;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import com.mirceone.inventoryapp.repository.NotificationRepository;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.email.EmailService;
import com.mirceone.inventoryapp.service.support.AfterCommitExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private FirmMemberRepository firmMemberRepository;
    @Mock
    private FirmRepository firmRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        notificationService = new NotificationService(
                notificationRepository,
                firmMemberRepository,
                firmRepository,
                userRepository,
                emailService,
                new ObjectMapper(),
                new AfterCommitExecutor(),
                transactionManager
        );
    }

    @Test
    void listNotificationsReturnsItemsAndUnreadCount() {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        NotificationEntity entity = new NotificationEntity(
                firmId,
                userId,
                NotificationType.PRODUCT_CREATED,
                com.mirceone.inventoryapp.model.NotificationLevel.INFO,
                "Produs adaugat",
                "Body",
                "{\"event\":\"product_created\"}"
        );
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(entity, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));

        when(notificationRepository.findAllByRecipientUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(notificationRepository.countByRecipientUserIdAndReadAtIsNull(userId)).thenReturn(3L);

        NotificationContracts.NotificationInbox inbox = notificationService.listNotifications(userId, false, 20);

        assertEquals(3L, inbox.unreadCount());
        assertEquals(1, inbox.items().size());
        assertEquals("product_created", inbox.items().getFirst().metadata().get("event"));
        assertFalse(inbox.items().getFirst().read());
    }

    @Test
    void markReadSetsReadAtWhenNotificationBelongsToUser() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationEntity entity = new NotificationEntity(
                UUID.randomUUID(),
                userId,
                NotificationType.PRODUCT_CREATED,
                com.mirceone.inventoryapp.model.NotificationLevel.INFO,
                "Title",
                "Body",
                null
        );

        when(notificationRepository.findByIdAndRecipientUserId(notificationId, userId)).thenReturn(Optional.of(entity));
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.markRead(userId, notificationId);

        assertTrue(entity.getReadAt() != null);
        verify(notificationRepository).save(entity);
    }

    @Test
    void notifyProductCreatedPersistsOneNotificationPerFirmMember() {
        UUID firmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        FirmEntity firm = new FirmEntity(UUID.randomUUID(), "Demo SRL");
        ReflectionTestUtils.setField(firm, "id", firmId);

        when(firmRepository.findById(firmId)).thenReturn(Optional.of(firm));
        when(firmMemberRepository.findAllByFirmIdOrderByCreatedAtAsc(firmId)).thenReturn(List.of(
                new FirmMemberEntity(firmId, userId1, MemberRole.OWNER),
                new FirmMemberEntity(firmId, userId2, MemberRole.MEMBER)
        ));

        notificationService.notifyProductCreatedAfterCommit(firmId, productId, "Laptop", "SKU-1", 10);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<NotificationEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<NotificationEntity> saved = StreamSupport.stream(captor.getValue().spliterator(), false).toList();

        assertEquals(2, saved.size());
        assertEquals(userId1, saved.getFirst().getRecipientUserId());
        assertEquals(NotificationType.PRODUCT_CREATED, saved.getFirst().getType());
        assertTrue(saved.getFirst().getMetadataJson().contains(productId.toString()));
    }

    @Test
    void notifyCriticalFirmStatusPersistsNotificationsAndEmailsOwner() {
        UUID firmId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        FirmEntity firm = new FirmEntity(ownerUserId, "Demo SRL");
        ReflectionTestUtils.setField(firm, "id", firmId);
        UserEntity owner = new UserEntity("owner@example.com", null, ProviderType.LOCAL, "owner@example.com", "Owner");
        ReflectionTestUtils.setField(owner, "id", ownerUserId);

        when(firmRepository.findById(firmId)).thenReturn(Optional.of(firm));
        when(firmMemberRepository.findAllByFirmIdOrderByCreatedAtAsc(firmId))
                .thenReturn(List.of(new FirmMemberEntity(firmId, ownerUserId, MemberRole.OWNER)));
        when(userRepository.findById(ownerUserId)).thenReturn(Optional.of(owner));

        notificationService.notifyFirmStatusChangedAfterCommit(
                firmId,
                FirmStatus.ACTIVE,
                FirmStatus.CRITICAL,
                "Ownership mismatch",
                FirmStatusChangeSource.SYSTEM
        );

        verify(notificationRepository).saveAll(any());
        verify(emailService).sendCriticalFirmStatusEmail(
                "owner@example.com",
                "Demo SRL",
                "Critical",
                "Ownership mismatch"
        );
    }
}
