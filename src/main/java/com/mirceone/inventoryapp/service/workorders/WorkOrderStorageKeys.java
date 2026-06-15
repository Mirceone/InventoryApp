package com.mirceone.inventoryapp.service.workorders;

import java.util.UUID;

/**
 * Opaque blob key scheme: {firmId}/{workOrderId}/{fileId}{suffix}.
 * Keys never encode the logical folder tree, so folder operations are pure DB updates
 * and a future S3/MinIO backend needs no rename support.
 */
public final class WorkOrderStorageKeys {

    private WorkOrderStorageKeys() {
    }

    public static String fileKey(UUID firmId, UUID workOrderId, UUID fileId, String fileSuffix) {
        return workOrderPrefix(firmId, workOrderId) + fileId + fileSuffix;
    }

    public static String workOrderPrefix(UUID firmId, UUID workOrderId) {
        return firmId + "/" + workOrderId + "/";
    }

    public static String firmPrefix(UUID firmId) {
        return firmId + "/";
    }
}
