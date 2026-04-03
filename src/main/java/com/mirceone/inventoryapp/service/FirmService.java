package com.mirceone.inventoryapp.service;

import com.mirceone.inventoryapp.api.firms.CreateFirmRequest;
import com.mirceone.inventoryapp.api.firms.FirmResponse;
import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmMemberEntity;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Service
public class FirmService {

    private final FirmRepository firmRepository;
    private final FirmMemberRepository firmMemberRepository;

    public FirmService(FirmRepository firmRepository, FirmMemberRepository firmMemberRepository) {
        this.firmRepository = firmRepository;
        this.firmMemberRepository = firmMemberRepository;
    }

    public FirmResponse createFirm(UUID userId, CreateFirmRequest request) {
        FirmEntity firm = new FirmEntity(userId, request.name());
        firm = firmRepository.save(firm);

        FirmMemberEntity member = new FirmMemberEntity(firm.getId(), userId, MemberRole.OWNER);
        firmMemberRepository.save(member);

        return new FirmResponse(firm.getId(), firm.getName());
    }

    public List<FirmResponse> getFirmsForUser(UUID userId) {
        List<UUID> firmIds = firmMemberRepository.findAllByUserId(userId)
                .stream()
                .map(FirmMemberEntity::getFirmId)
                .toList();

        return firmRepository.findAllByIdIn(firmIds)
                .stream()
                .map(f -> new FirmResponse(f.getId(), f.getName()))
                .toList();
    }

    public void assertUserIsMember(UUID firmId, UUID userId) {
        boolean ok = firmMemberRepository.existsByFirmIdAndUserId(firmId, userId);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this firm");
        }
    }
}
