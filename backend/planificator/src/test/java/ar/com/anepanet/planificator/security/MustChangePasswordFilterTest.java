package ar.com.anepanet.planificator.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MustChangePasswordFilterTest {

    @Test
    void allowsOnlyRequiredAuthenticationActions() {
        assertTrue(MustChangePasswordFilter.allowedWhileMustChange(
                "GET", "/api/auth/me"));
        assertTrue(MustChangePasswordFilter.allowedWhileMustChange(
                "POST", "/api/auth/change-password"));
        assertTrue(MustChangePasswordFilter.allowedWhileMustChange(
                "POST", "/api/auth/logout"));
    }

    @Test
    void preservesPublicReadOnlyEndpoints() {
        assertTrue(MustChangePasswordFilter.allowedWhileMustChange(
                "GET", "/api/tasks/board"));
        assertTrue(MustChangePasswordFilter.allowedWhileMustChange(
                "GET", "/api/tasks/84b41d4e-5cd8-47d7-9cdc-ff170d1f8298/history"));
        assertTrue(MustChangePasswordFilter.allowedWhileMustChange(
                "GET", "/api/shifts"));
    }

    @Test
    void blocksMutationsAndPrivateReads() {
        assertFalse(MustChangePasswordFilter.allowedWhileMustChange(
                "POST", "/api/tasks/task-id/move"));
        assertFalse(MustChangePasswordFilter.allowedWhileMustChange(
                "GET", "/api/users"));
        assertFalse(MustChangePasswordFilter.allowedWhileMustChange(
                "DELETE", "/api/shifts/shift-id"));
    }
}
