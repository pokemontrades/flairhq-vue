package org.ptrades.flairhq.service;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("modSecurity")
public class ModSecurityService {

    /**
     * Returns true if the session carries the given mod permission authority.
     * Used in @PreAuthorize expressions: @modSecurity.hasPermission(authentication, 'flair')
     * Authorities are set from Reddit's API at login and live in the Spring session only —
     * no DB read occurs here, so a database write cannot escalate privileges.
     */
    public boolean hasPermission(@NonNull Authentication authentication, @NonNull String permission) {
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(a -> a.equals("MOD_PERM_all") || a.equals("MOD_PERM_" + permission));
    }

}
