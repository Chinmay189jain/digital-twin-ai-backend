package com.digitaltwin.backend.controller;

import com.digitaltwin.backend.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteMyAccount() {
        accountService.deleteCurrentUserAccount();
        return ResponseEntity.noContent().build();
    }
}
