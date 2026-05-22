package com.mirceone.inventoryapp.integration;

import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.auth.AuthContracts;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.firms.FirmContracts;
import com.mirceone.inventoryapp.service.firms.FirmService;
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
}
