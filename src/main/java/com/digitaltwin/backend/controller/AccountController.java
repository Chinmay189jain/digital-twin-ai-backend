package com.digitaltwin.backend.controller;

import com.digitaltwin.backend.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteMyAccount() {
        accountService.deleteCurrentUserAccount();
        return ResponseEntity.noContent().build();
    }
}
