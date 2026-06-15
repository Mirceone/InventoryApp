package com.mirceone.inventoryapp.api.firms.members;

import com.mirceone.inventoryapp.api.support.CurrentUserId;
import com.mirceone.inventoryapp.service.firms.members.FirmInvitationService;
import com.mirceone.inventoryapp.service.firms.members.FirmMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/firms/{firmId}")
@Tag(name = "Firm members", description = "Team members and invitations")
@SecurityRequirement(name = "bearerAuth")
public class FirmInvitationController {

    private final FirmInvitationService firmInvitationService;
    private final FirmMemberService firmMemberService;

    public FirmInvitationController(FirmInvitationService firmInvitationService, FirmMemberService firmMemberService) {
        this.firmInvitationService = firmInvitationService;
        this.firmMemberService = firmMemberService;
    }

    @GetMapping("/members")
    @Operation(summary = "List firm members (owner only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Members returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public List<FirmMemberResponse> listMembers(@CurrentUserId UUID userId, @PathVariable UUID firmId) {
        return FirmMembersWebMapper.toMemberResponseList(firmMemberService.listMembers(firmId, userId));
    }

    @PatchMapping("/members/{memberUserId}/role")
    @Operation(summary = "Update a member role (owner only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Member role updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Member not found")
    })
    public FirmMemberResponse updateMemberRole(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID memberUserId,
            @Valid @RequestBody UpdateMemberRoleRequest request
    ) {
        return FirmMembersWebMapper.toMemberResponse(
                firmMemberService.updateMemberRole(
                        firmId,
                        userId,
                        memberUserId,
                        FirmMembersWebMapper.toUpdateRoleSpec(request)
                )
        );
    }

    @DeleteMapping("/members/{memberUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a member from the firm (owner only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member removed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Member not found")
    })
    public void removeMember(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID memberUserId
    ) {
        firmMemberService.removeMember(firmId, userId, memberUserId);
    }

    @PostMapping("/ownership/transfer")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Start ownership transfer confirmation by email code")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Confirmation code email sent"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Member or firm not found")
    })
    public void requestOwnershipTransfer(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @Valid @RequestBody TransferOwnershipRequest request
    ) {
        firmMemberService.requestOwnershipTransfer(
                firmId,
                userId,
                FirmMembersWebMapper.toTransferOwnershipSpec(request)
        );
    }

    @PostMapping("/ownership/transfer/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Confirm ownership transfer using the emailed 6-digit code")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ownership transferred"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired confirmation code"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Member or firm not found")
    })
    public void confirmOwnershipTransfer(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @Valid @RequestBody ConfirmOwnershipTransferRequest request
    ) {
        firmMemberService.confirmOwnershipTransfer(
                firmId,
                userId,
                FirmMembersWebMapper.toConfirmOwnershipTransferSpec(request)
        );
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invite a member by email (owner only)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitation created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "409", description = "Already member or pending invite exists")
    })
    public FirmInvitationResponse createInvitation(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @Valid @RequestBody InviteMemberRequest request
    ) {
        return FirmMembersWebMapper.toInvitationResponse(
                firmInvitationService.createInvitation(firmId, userId, FirmMembersWebMapper.toCreateSpec(request))
                        .summary()
        );
    }

    @GetMapping("/invitations")
    @Operation(summary = "List pending invitations (owner only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitations returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public List<FirmInvitationResponse> listPendingInvitations(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId
    ) {
        return FirmMembersWebMapper.toInvitationResponseList(
                firmInvitationService.listPendingInvitations(firmId, userId)
        );
    }

    @DeleteMapping("/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a pending invitation (owner only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Invitation revoked"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Invitation not found")
    })
    public void revokeInvitation(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID invitationId
    ) {
        firmInvitationService.revokeInvitation(firmId, userId, invitationId);
    }
}
