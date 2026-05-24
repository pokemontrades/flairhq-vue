package org.ptrades.flairhq.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModSecurityServiceTest {

    private final ModSecurityService service = new ModSecurityService();

    private Authentication auth(String... authorities) {
        Authentication auth = mock(Authentication.class);
        Collection<GrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a))
                .collect(Collectors.toList());
        doReturn(grantedAuthorities).when(auth).getAuthorities();
        return auth;
    }

    @Test
    void hasPermission_modPermAll_returnsTrue() {
        assertTrue(service.hasPermission(auth("MOD_PERM_all"), "flair"));
    }

    @Test
    void hasPermission_modPermAll_trueForAnyPermission() {
        assertTrue(service.hasPermission(auth("MOD_PERM_all"), "ban"));
    }

    @Test
    void hasPermission_exactPermissionMatch_returnsTrue() {
        assertTrue(service.hasPermission(auth("MOD_PERM_flair"), "flair"));
    }

    @Test
    void hasPermission_differentPermission_returnsFalse() {
        assertFalse(service.hasPermission(auth("MOD_PERM_ban"), "flair"));
    }

    @Test
    void hasPermission_noAuthorities_returnsFalse() {
        assertFalse(service.hasPermission(auth(), "flair"));
    }

    @Test
    void hasPermission_partialPermissionName_returnsFalse() {
        // "MOD_PERM_fl" should not match "MOD_PERM_flair"
        assertFalse(service.hasPermission(auth("MOD_PERM_fl"), "flair"));
    }

    @Test
    void hasPermission_multipleAuthorities_matchesFirst() {
        assertTrue(service.hasPermission(auth("ROLE_USER", "MOD_PERM_flair"), "flair"));
    }

    @Test
    void hasPermission_modPermAllAmongOthers_returnsTrue() {
        assertTrue(service.hasPermission(auth("ROLE_USER", "MOD_PERM_all"), "anything"));
    }
}
