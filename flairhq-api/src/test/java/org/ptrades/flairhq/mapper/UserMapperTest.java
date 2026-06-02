package org.ptrades.flairhq.mapper;

import org.junit.jupiter.api.Test;
import org.ptrades.flairhq.dto.UserRequest;
import org.ptrades.flairhq.dto.UserResponse;
import org.ptrades.flairhq.repository.domain.User;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    // ── toNewUser ─────────────────────────────────────────────────────────────

    @Test
    void toNewUser_ignoresHideReciprocalSection() {
        UserRequest req = new UserRequest();
        req.setHideReciprocalSection(true);

        User user = mapper.toNewUser(req, "alice");

        assertNull(user.getHideReciprocalSection());
    }

    @Test
    void toNewUser_setsIdAndDefaults() {
        User user = mapper.toNewUser(new UserRequest(), "alice");

        assertEquals("alice", user.getId());
        assertFalse(user.getIsMod());
        assertFalse(user.getBanned());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    // ── applyUpdate ───────────────────────────────────────────────────────────

    @Test
    void applyUpdate_setsHideReciprocalSection_true() {
        User user = new User();
        UserRequest req = new UserRequest();
        req.setHideReciprocalSection(true);

        mapper.applyUpdate(req, user);

        assertTrue(user.getHideReciprocalSection());
    }

    @Test
    void applyUpdate_setsHideReciprocalSection_false() {
        User user = new User();
        user.setHideReciprocalSection(true);
        UserRequest req = new UserRequest();
        req.setHideReciprocalSection(false);

        mapper.applyUpdate(req, user);

        assertFalse(user.getHideReciprocalSection());
    }

    @Test
    void applyUpdate_nullHideReciprocalSection_clearsField() {
        User user = new User();
        user.setHideReciprocalSection(true);
        UserRequest req = new UserRequest();
        // hideReciprocalSection left null — caller sends only fields they want to change

        mapper.applyUpdate(req, user);

        assertNull(user.getHideReciprocalSection());
    }

    @Test
    void applyUpdate_preservesUnrelatedFields() {
        User user = new User();
        user.setId("alice");
        user.setIsMod(true);
        UserRequest req = new UserRequest();
        req.setHideReciprocalSection(true);

        mapper.applyUpdate(req, user);

        assertEquals("alice", user.getId());
        assertTrue(user.getIsMod());
    }

    // ── toResponse ────────────────────────────────────────────────────────────

    @Test
    void toResponse_includesHideReciprocalSection_true() {
        User user = new User();
        user.setHideReciprocalSection(true);

        UserResponse response = mapper.toResponse(user);

        assertTrue(response.getHideReciprocalSection());
    }

    @Test
    void toResponse_includesHideReciprocalSection_null() {
        User user = new User();

        UserResponse response = mapper.toResponse(user);

        assertNull(response.getHideReciprocalSection());
    }
}
