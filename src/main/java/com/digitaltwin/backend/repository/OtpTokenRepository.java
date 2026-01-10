package com.digitaltwin.backend.repository;

import com.digitaltwin.backend.service.OtpPurpose;
import com.digitaltwin.backend.model.OtpToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpTokenRepository extends MongoRepository<OtpToken, String> {

    Optional<OtpToken> findByEmailAndPurpose(String email, OtpPurpose purpose);

    void deleteByEmailAndPurpose(String email, OtpPurpose purpose);
}
