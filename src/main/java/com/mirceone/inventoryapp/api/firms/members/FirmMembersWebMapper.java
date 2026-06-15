package com.mirceone.inventoryapp.api.firms.members;

import com.mirceone.inventoryapp.service.firms.members.FirmInvitationContracts;
import com.mirceone.inventoryapp.service.firms.members.FirmMemberContracts;

import java.util.List;

public final class FirmMembersWebMapper {

    private FirmMembersWebMapper() {
    }

    public static FirmInvitationContracts.CreateInvitationSpec toCreateSpec(InviteMemberRequest request) {
        return new FirmInvitationContracts.CreateInvitationSpec(request.email(), request.role());
    }

    public static FirmInvitationResponse toInvitationResponse(FirmInvitationContracts.InvitationSummary summary) {
        return new FirmInvitationResponse(
                summary.id(),
                summary.email(),
                summary.role().name(),
                summary.roleDisplayLabel(),
                summary.status().name(),
                summary.expiresAt(),
                summary.createdAt()
        );
    }

    public static List<FirmInvitationResponse> toInvitationResponseList(
            List<FirmInvitationContracts.InvitationSummary> summaries
    ) {
        return summaries.stream().map(FirmMembersWebMapper::toInvitationResponse).toList();
    }

    public static FirmMemberResponse toMemberResponse(FirmMemberContracts.FirmMemberSummary summary) {
        return new FirmMemberResponse(
                summary.userId(),
                summary.email(),
                summary.displayName(),
                summary.role().name(),
                summary.roleDisplayLabel(),
                summary.joinedAt()
        );
    }

    public static List<FirmMemberResponse> toMemberResponseList(List<FirmMemberContracts.FirmMemberSummary> summaries) {
        return summaries.stream().map(FirmMembersWebMapper::toMemberResponse).toList();
    }

    public static FirmMemberContracts.UpdateMemberRoleSpec toUpdateRoleSpec(UpdateMemberRoleRequest request) {
        return new FirmMemberContracts.UpdateMemberRoleSpec(request.role());
    }

    public static FirmMemberContracts.TransferOwnershipSpec toTransferOwnershipSpec(TransferOwnershipRequest request) {
        return new FirmMemberContracts.TransferOwnershipSpec(request.newOwnerUserId());
    }

    public static FirmMemberContracts.ConfirmOwnershipTransferSpec toConfirmOwnershipTransferSpec(
            ConfirmOwnershipTransferRequest request
    ) {
        return new FirmMemberContracts.ConfirmOwnershipTransferSpec(request.newOwnerUserId(), request.confirmationCode());
    }
}
