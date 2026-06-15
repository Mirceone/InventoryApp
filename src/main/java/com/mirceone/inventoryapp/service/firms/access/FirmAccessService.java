package com.mirceone.inventoryapp.service.firms.access;

import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmMemberEntity;
import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class FirmAccessService {

    private final FirmMemberRepository firmMemberRepository;
    private final FirmRepository firmRepository;

    public FirmAccessService(FirmMemberRepository firmMemberRepository, FirmRepository firmRepository) {
        this.firmMemberRepository = firmMemberRepository;
        this.firmRepository = firmRepository;
    }

    public void requireMembership(UUID firmId, UUID userId) {
        resolveMembership(firmId, userId);
    }

    public void requireOwner(UUID firmId, UUID userId) {
        requirePermission(firmId, userId, FirmPermission.FIRM_UPDATE);
    }

    public void requirePermission(UUID firmId, UUID userId, FirmPermission permission) {
        FirmMembership membership = resolveMembership(firmId, userId);
        if (!RolePermissions.allowed(membership.role(), permission)) {
            throw forbiddenForPermission(permission);
        }
    }

    public void requireOperationalPermission(UUID firmId, UUID userId, FirmPermission permission) {
        requirePermission(firmId, userId, permission);
        requireFirmOperational(firmId);
    }

    public void requireFirmOperational(UUID firmId) {
        FirmEntity firm = firmRepository.findById(firmId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Firm not found"));
        if (firm.getStatus() != FirmStatus.ACTIVE) {
            throw notOperational(firm);
        }
    }

    public void requireFirmOperationalForUser(UUID firmId, UUID userId) {
        resolveMembership(firmId, userId);
        requireFirmOperational(firmId);
    }

    public FirmMembership resolveMembership(UUID firmId, UUID userId) {
        FirmMemberEntity member = firmMemberRepository.findByFirmIdAndUserId(firmId, userId)
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Not a member of this firm"));
        return new FirmMembership(member.getFirmId(), member.getUserId(), member.getRole());
    }

    private static ResponseStatusException notOperational(FirmEntity firm) {
        if (firm.getStatus() == FirmStatus.PAUSED) {
            return new ResponseStatusException(FORBIDDEN, "Firm operations are paused");
        }
        if (firm.getStatus() == FirmStatus.CRITICAL) {
            String detail = firm.getStatusMessage();
            if (detail != null && !detail.isBlank()) {
                return new ResponseStatusException(FORBIDDEN, "Firm is in critical state: " + detail.strip());
            }
            return new ResponseStatusException(FORBIDDEN, "Firm is in critical state");
        }
        return new ResponseStatusException(FORBIDDEN, "Firm is not operational");
    }

    private static ResponseStatusException forbiddenForPermission(FirmPermission permission) {
        if (permission == FirmPermission.FIRM_UPDATE || permission == FirmPermission.FIRM_DELETE) {
            return new ResponseStatusException(FORBIDDEN, "Only the firm owner can perform this action");
        }
        if (permission == FirmPermission.MEMBER_MANAGE) {
            return new ResponseStatusException(FORBIDDEN, "Only the firm owner can manage members");
        }
        return new ResponseStatusException(FORBIDDEN, "Insufficient permissions for this firm");
    }
}
