package com.mirceone.inventoryapp.integration;

import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.auth.AuthContracts;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.firms.FirmContracts;
import com.mirceone.inventoryapp.service.firms.FirmService;
import com.mirceone.inventoryapp.service.firms.members.FirmMemberContracts;
import com.mirceone.inventoryapp.service.firms.members.FirmMemberService;
import com.mirceone.inventoryapp.service.firms.status.FirmOwnershipConsistencyWorker;
import com.mirceone.inventoryapp.service.inventory.InventoryContracts;
import com.mirceone.inventoryapp.service.inventory.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class FirmServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FirmService firmService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private FirmMemberService firmMemberService;
    @Autowired
    private FirmOwnershipConsistencyWorker firmOwnershipConsistencyWorker;

    @Test
    void ownerCanRenameFirm() {
        authService.signup(new AuthContracts.SignupSpec("firm-rename@example.com", "password123", "Owner"));
        UserEntity user = userRepository.findByEmailIgnoreCase("firm-rename@example.com").orElseThrow();
        UUID userId = user.getId();

        FirmContracts.FirmSummary created =
                firmService.createFirm(userId, new FirmContracts.CreateFirmSpec("  Alpha SRL  "));
        assertEquals("Alpha SRL", created.name());
        assertEquals(MemberRole.OWNER, created.role());
        assertEquals("Admin", created.roleDisplayLabel());
        assertEquals(FirmStatus.ACTIVE, created.status());

        FirmContracts.FirmSummary renamed =
                firmService.renameFirm(userId, created.id(), new FirmContracts.UpdateFirmSpec("Beta SRL"));
        assertEquals("Beta SRL", renamed.name());

        String nameInDb = jdbcTemplate.queryForObject(
                "SELECT name FROM firms WHERE id = ?",
                String.class,
                created.id()
        );
        assertEquals("Beta SRL", nameInDb);
    }

    @Test
    void ownerCanDeleteFirmAndCascadeData() {
        authService.signup(new AuthContracts.SignupSpec("firm-delete@example.com", "password123", "Owner"));
        UserEntity user = userRepository.findByEmailIgnoreCase("firm-delete@example.com").orElseThrow();
        UUID userId = user.getId();

        FirmContracts.FirmSummary created =
                firmService.createFirm(userId, new FirmContracts.CreateFirmSpec("To Delete"));

        firmService.deleteFirm(userId, created.id());

        Integer firmCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM firms WHERE id = ?",
                Integer.class,
                created.id()
        );
        assertEquals(0, firmCount);

        Integer memberCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM firm_members WHERE firm_id = ?",
                Integer.class,
                created.id()
        );
        assertEquals(0, memberCount);
    }

    @Test
    void creatorListedAsOwnerWithDisplayLabel() {
        authService.signup(new AuthContracts.SignupSpec("firm-list@example.com", "password123", "Owner"));
        UserEntity user = userRepository.findByEmailIgnoreCase("firm-list@example.com").orElseThrow();
        UUID userId = user.getId();

        firmService.createFirm(userId, new FirmContracts.CreateFirmSpec("Listed Firm"));

        FirmContracts.FirmSummary listed = firmService.getFirmsForUser(userId).getFirst();
        assertEquals("Listed Firm", listed.name());
        assertEquals(MemberRole.OWNER, listed.role());
        assertEquals("Admin", listed.roleDisplayLabel());
    }

    @Test
    void pausedFirmBlocksInventoryOperations() {
        authService.signup(new AuthContracts.SignupSpec("firm-pause@example.com", "password123", "Owner"));
        UserEntity user = userRepository.findByEmailIgnoreCase("firm-pause@example.com").orElseThrow();
        UUID userId = user.getId();

        FirmContracts.FirmSummary created =
                firmService.createFirm(userId, new FirmContracts.CreateFirmSpec("Paused Firm"));

        firmService.updateFirmStatus(
                userId, created.id(), new FirmContracts.UpdateFirmStatusSpec(FirmStatus.PAUSED, null)
        );

        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> inventoryService.createProduct(
                        userId,
                        created.id(),
                        new InventoryContracts.CreateProductSpec("Item", null, 1, null, null, null, null, null)
                )
        );

        firmService.updateFirmStatus(
                userId, created.id(), new FirmContracts.UpdateFirmStatusSpec(FirmStatus.ACTIVE, null)
        );

        InventoryContracts.ProductSummary product = inventoryService.createProduct(
                userId,
                created.id(),
                new InventoryContracts.CreateProductSpec("Item", null, 1, null, null, null, null, null)
        );
        assertEquals("Item", product.name());
    }

    @Test
    void setFirmStatusSystemCanSetCritical() {
        authService.signup(new AuthContracts.SignupSpec("firm-sys@example.com", "password123", "Owner"));
        UserEntity user = userRepository.findByEmailIgnoreCase("firm-sys@example.com").orElseThrow();

        FirmContracts.FirmSummary created =
                firmService.createFirm(user.getId(), new FirmContracts.CreateFirmSpec("System Firm"));

        firmService.setFirmStatusSystem(created.id(), FirmStatus.CRITICAL, "Integrity check failed");

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM firms WHERE id = ?",
                String.class,
                created.id()
        );
        assertEquals("CRITICAL", status);
    }

    @Test
    void deleteFirmWhenNotOwnerReturnsForbidden() {
        authService.signup(new AuthContracts.SignupSpec("firm-owner@example.com", "password123", "Owner"));
        UserEntity owner = userRepository.findByEmailIgnoreCase("firm-owner@example.com").orElseThrow();

        FirmContracts.FirmSummary created =
                firmService.createFirm(owner.getId(), new FirmContracts.CreateFirmSpec("Shared Firm"));

        authService.signup(new AuthContracts.SignupSpec("firm-member@example.com", "password456", "Member"));
        UserEntity member = userRepository.findByEmailIgnoreCase("firm-member@example.com").orElseThrow();

        jdbcTemplate.update(
                "INSERT INTO firm_members (firm_id, user_id, role) VALUES (?, ?, 'MEMBER')",
                created.id(),
                member.getId()
        );

        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> firmService.deleteFirm(member.getId(), created.id())
        );
    }

    @Test
    void confirmOwnershipTransferUpdatesOwnerReferenceAndMembershipRoles() {
        authService.signup(new AuthContracts.SignupSpec("transfer-owner@example.com", "password123", "Owner"));
        authService.signup(new AuthContracts.SignupSpec("transfer-member@example.com", "password123", "Member"));
        UserEntity owner = userRepository.findByEmailIgnoreCase("transfer-owner@example.com").orElseThrow();
        UserEntity member = userRepository.findByEmailIgnoreCase("transfer-member@example.com").orElseThrow();

        FirmContracts.FirmSummary created =
                firmService.createFirm(owner.getId(), new FirmContracts.CreateFirmSpec("Transfer Firm"));

        jdbcTemplate.update(
                "INSERT INTO firm_members (firm_id, user_id, role) VALUES (?, ?, 'MEMBER')",
                created.id(),
                member.getId()
        );

        FirmMemberContracts.TransferOwnershipRequestResult requestResult = firmMemberService.requestOwnershipTransfer(
                created.id(),
                owner.getId(),
                new FirmMemberContracts.TransferOwnershipSpec(member.getId())
        );

        firmMemberService.confirmOwnershipTransfer(
                created.id(),
                owner.getId(),
                new FirmMemberContracts.ConfirmOwnershipTransferSpec(member.getId(), requestResult.rawConfirmationCode())
        );

        UUID ownerUserId = jdbcTemplate.queryForObject(
                "SELECT owner_user_id FROM firms WHERE id = ?",
                UUID.class,
                created.id()
        );
        assertEquals(member.getId(), ownerUserId);

        String previousOwnerRole = jdbcTemplate.queryForObject(
                "SELECT role FROM firm_members WHERE firm_id = ? AND user_id = ?",
                String.class,
                created.id(),
                owner.getId()
        );
        String newOwnerRole = jdbcTemplate.queryForObject(
                "SELECT role FROM firm_members WHERE firm_id = ? AND user_id = ?",
                String.class,
                created.id(),
                member.getId()
        );
        assertEquals("MEMBER", previousOwnerRole);
        assertEquals("OWNER", newOwnerRole);
    }

    @Test
    void statusHistoryRecordsManualAndSystemTransitions() {
        authService.signup(new AuthContracts.SignupSpec("firm-history@example.com", "password123", "Owner"));
        UserEntity owner = userRepository.findByEmailIgnoreCase("firm-history@example.com").orElseThrow();

        FirmContracts.FirmSummary created =
                firmService.createFirm(owner.getId(), new FirmContracts.CreateFirmSpec("History Firm"));

        firmService.updateFirmStatus(
                owner.getId(),
                created.id(),
                new FirmContracts.UpdateFirmStatusSpec(FirmStatus.PAUSED, "Manual pause")
        );
        firmService.setFirmStatusSystem(created.id(), FirmStatus.CRITICAL, "System critical");

        Integer historyCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM firm_status_history WHERE firm_id = ?",
                Integer.class,
                created.id()
        );
        assertEquals(2, historyCount);

        String latestSource = jdbcTemplate.queryForObject(
                "SELECT source FROM firm_status_history WHERE firm_id = ? ORDER BY created_at DESC LIMIT 1",
                String.class,
                created.id()
        );
        assertEquals("SYSTEM", latestSource);
    }

    @Test
    void ownershipConsistencyWorkerMarksFirmCriticalWhenOwnerMembershipDrifts() {
        authService.signup(new AuthContracts.SignupSpec("firm-drift@example.com", "password123", "Owner"));
        UserEntity owner = userRepository.findByEmailIgnoreCase("firm-drift@example.com").orElseThrow();

        FirmContracts.FirmSummary created =
                firmService.createFirm(owner.getId(), new FirmContracts.CreateFirmSpec("Drift Firm"));

        jdbcTemplate.update(
                "UPDATE firm_members SET role = 'MEMBER' WHERE firm_id = ? AND user_id = ?",
                created.id(),
                owner.getId()
        );

        firmOwnershipConsistencyWorker.verifyOwnershipConsistency();

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM firms WHERE id = ?",
                String.class,
                created.id()
        );
        String latestMessage = jdbcTemplate.queryForObject(
                "SELECT message FROM firm_status_history WHERE firm_id = ? ORDER BY created_at DESC LIMIT 1",
                String.class,
                created.id()
        );
        assertEquals("CRITICAL", status);
        assertEquals(FirmOwnershipConsistencyWorker.INCONSISTENT_OWNER_MESSAGE, latestMessage);
    }
}
