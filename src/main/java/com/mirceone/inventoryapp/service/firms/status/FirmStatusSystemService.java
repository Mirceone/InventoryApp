package com.mirceone.inventoryapp.service.firms.status;

import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.service.firms.FirmService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FirmStatusSystemService {

    private final FirmService firmService;

    public FirmStatusSystemService(FirmService firmService) {
        this.firmService = firmService;
    }

    public void markCritical(UUID firmId, String message) {
        firmService.setFirmStatusSystem(firmId, FirmStatus.CRITICAL, message);
    }
}
