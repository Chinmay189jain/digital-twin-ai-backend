package com.digitaltwin.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    @Autowired
    private TwinChatService twinChatService;

    @Autowired
    private TwinChatSessionService twinChatSessionService;

    @Autowired
    private TwinProfileService twinProfileService;

    @Autowired
    private UserService userService;

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
