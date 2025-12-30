package com.digitaltwin.backend.repository;

import com.digitaltwin.backend.model.OtpToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpTokenRepository extends MongoRepository<OtpToken, String> {
    Optional<OtpToken> findByEmail(String email);
}
