package com.digitaltwin.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final TwinChatService twinChatService;

    private final TwinChatSessionService twinChatSessionService;

    private final TwinProfileService twinProfileService;

    private final UserService userService;

    @Transactional
    public void deleteCurrentUserAccount() {
        String email = userService.getCurrentUserEmail();

        // Delete all TwinChats associated with the user
        twinChatService.deleteAllChatsByUserId(email);

        // Delete all TwinChatSessions associated with the user
        twinChatSessionService.deleteAllSessionsByUserId(email);

        // Delete all TwinProfiles associated with the user
        twinProfileService.deleteAllProfilesByUserId(email);

        // Finally, delete the user account
        userService.deleteByEmail(email);
    }
}
