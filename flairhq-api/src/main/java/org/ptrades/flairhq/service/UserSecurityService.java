package org.ptrades.flairhq.service;

import org.ptrades.flairhq.repository.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurityService {

    private final UserRepository userRepository;

    public UserSecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Used in @PreAuthorize expressions to confirm ownership against the
     * User collection. Returns true if no document exists yet (first-time
     * creation is allowed) or if the found document's id matches the
     * authenticated username.
     */
    public boolean isOwner(@NonNull String username) {
        return userRepository.findById(username)
                .map(user -> user.getId().equals(username))
                .orElse(true);
    }

}
