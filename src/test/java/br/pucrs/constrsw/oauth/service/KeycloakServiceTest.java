package br.pucrs.constrsw.oauth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakServiceTest {

    private KeycloakService keycloakService;

    @BeforeEach
    void setUp() {
        keycloakService = new KeycloakService();
    }

    @Test
    void testAdministratorAccess() {
        List<String> adminRoles = List.of("administrator");

        assertTrue(keycloakService.hasAccessToResource(adminRoles, "resources"));
        assertTrue(keycloakService.hasAccessToResource(adminRoles, "rooms"));
        assertTrue(keycloakService.hasAccessToResource(adminRoles, "/professors"));
        assertTrue(keycloakService.hasAccessToResource(adminRoles, "students"));

        assertFalse(keycloakService.hasAccessToResource(adminRoles, "courses"));
        assertFalse(keycloakService.hasAccessToResource(adminRoles, "lessons"));
    }

    @Test
    void testCoordinatorAccess() {
        List<String> coordinatorRoles = List.of("coordinator");

        assertTrue(keycloakService.hasAccessToResource(coordinatorRoles, "courses"));
        assertTrue(keycloakService.hasAccessToResource(coordinatorRoles, "/classes"));

        assertFalse(keycloakService.hasAccessToResource(coordinatorRoles, "rooms"));
        assertFalse(keycloakService.hasAccessToResource(coordinatorRoles, "lessons"));
    }

    @Test
    void testProfessorAccess() {
        List<String> professorRoles = List.of("professor");

        assertTrue(keycloakService.hasAccessToResource(professorRoles, "lessons"));
        assertTrue(keycloakService.hasAccessToResource(professorRoles, "/reservations"));

        assertFalse(keycloakService.hasAccessToResource(professorRoles, "students"));
        assertFalse(keycloakService.hasAccessToResource(professorRoles, "courses"));
    }

    @Test
    void testStudentAccess() {
        List<String> studentRoles = List.of("student");

        assertFalse(keycloakService.hasAccessToResource(studentRoles, "lessons"));
        assertFalse(keycloakService.hasAccessToResource(studentRoles, "courses"));
        assertFalse(keycloakService.hasAccessToResource(studentRoles, "rooms"));
    }

    @Test
    void testNormalizeResource() {
        assertEquals("lessons", keycloakService.normalizeResource("/lessons"));
        assertEquals("courses", keycloakService.normalizeResource("COURSES"));
        assertEquals("rooms", keycloakService.normalizeResource("/ROOMS/"));
    }
}
