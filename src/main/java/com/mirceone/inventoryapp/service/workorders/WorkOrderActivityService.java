package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.model.WorkOrderActivityEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.repository.WorkOrderActivityRepository;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.firms.access.FirmPermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class WorkOrderActivityService {

    private final FirmAccessService firmAccessService;
    private final WorkOrderService workOrderService;
    private final WorkOrderActivityRepository activityRepository;
    private final UserRepository userRepository;

    public WorkOrderActivityService(
            FirmAccessService firmAccessService,
            WorkOrderService workOrderService,
            WorkOrderActivityRepository activityRepository,
            UserRepository userRepository
    ) {
        this.firmAccessService = firmAccessService;
        this.workOrderService = workOrderService;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkOrderActivitySummary> listActivity(UUID userId, UUID firmId, UUID workOrderId) {
        requireAccess(userId, firmId, workOrderId);
        List<WorkOrderActivityEntity> entries = activityRepository.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId);
        Map<UUID, UserEntity> authors = authorsById(entries);
        return entries.stream()
                .map(entry -> toSummary(entry, authors))
                .toList();
    }

    @Transactional
    public WorkOrderActivitySummary createActivity(
            UUID userId,
            UUID firmId,
            UUID workOrderId,
            WorkOrderContracts.CreateActivitySpec spec
    ) {
        requireAccess(userId, firmId, workOrderId);

        String title = WorkOrderTextSanitizer.sanitizeRequiredName(spec.title(), "Title");
        String description = sanitizeDescription(spec.description());

        WorkOrderActivityEntity saved = activityRepository.save(new WorkOrderActivityEntity(
                workOrderId,
                firmId,
                title,
                description,
                userId
        ));
        return toSummary(saved, authorsById(List.of(saved)));
    }

    private void requireAccess(UUID userId, UUID firmId, UUID workOrderId) {
        workOrderService.assertWorkOrderEnabled();
        firmAccessService.requireOperationalPermission(firmId, userId, FirmPermission.DOCUMENT_WRITE);
        workOrderService.requireWorkOrder(firmId, workOrderId);
    }

    private static String sanitizeDescription(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.strip();
        if (value.length() > 1000) {
            throw new ResponseStatusException(BAD_REQUEST, "Description must be at most 1000 characters");
        }
        return value;
    }

    private Map<UUID, UserEntity> authorsById(List<WorkOrderActivityEntity> entries) {
        List<UUID> userIds = entries.stream()
                .map(WorkOrderActivityEntity::getCreatedByUserId)
                .distinct()
                .toList();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity(), (a, b) -> a));
    }

    private static WorkOrderActivitySummary toSummary(WorkOrderActivityEntity entry, Map<UUID, UserEntity> authors) {
        UserEntity author = authors.get(entry.getCreatedByUserId());
        String email = author != null && author.getEmail() != null ? author.getEmail() : "";
        String displayName = author != null ? author.getDisplayName() : null;
        return new WorkOrderActivitySummary(
                entry.getId(),
                entry.getWorkOrderId(),
                entry.getTitle(),
                entry.getDescription(),
                entry.getCreatedByUserId(),
                email,
                displayName,
                entry.getCreatedAt()
        );
    }
}
