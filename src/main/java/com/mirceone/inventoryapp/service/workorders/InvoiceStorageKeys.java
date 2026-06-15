package com.mirceone.inventoryapp.service.workorders;

import java.util.UUID;

/**
 * Opaque blob keys for invoices: {firmId}/{workOrderId}/invoices/{invoiceId}{suffix}.
 */
public final class InvoiceStorageKeys {

    private InvoiceStorageKeys() {
    }

    public static String invoiceKey(UUID firmId, UUID workOrderId, UUID invoiceId, String fileSuffix) {
        return workOrderInvoicesPrefix(firmId, workOrderId) + invoiceId + fileSuffix;
    }

    public static String workOrderInvoicesPrefix(UUID firmId, UUID workOrderId) {
        return firmId + "/" + workOrderId + "/invoices/";
    }
}
