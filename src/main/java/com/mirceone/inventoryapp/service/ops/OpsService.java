package com.mirceone.inventoryapp.service.ops;

import com.mirceone.inventoryapp.model.OpsEventEntity;
import com.mirceone.inventoryapp.ops.MaintenanceLogRing;
import com.mirceone.inventoryapp.repository.OpsEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OpsService {

    private static final int MAX_LIMIT = 200;

    private final MaintenanceLogRing maintenanceLogRing;
    private final OpsEventRepository opsEventRepository;

    public OpsService(MaintenanceLogRing maintenanceLogRing, OpsEventRepository opsEventRepository) {
        this.maintenanceLogRing = maintenanceLogRing;
        this.opsEventRepository = opsEventRepository;
    }

    public List<String> recentLogs(int limit) {
        return maintenanceLogRing.recent(normalizeLimit(limit));
    }

    public List<OpsEventEntity> recentEvents(int limit) {
        return opsEventRepository.findAll(PageRequest.of(
                0,
                normalizeLimit(limit),
                Sort.by(Sort.Direction.DESC, "createdAt")
        )).getContent();
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
