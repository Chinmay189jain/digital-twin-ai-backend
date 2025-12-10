package com.digitaltwin.backend.repository;

import com.digitaltwin.backend.model.TwinProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TwinProfileRepository extends MongoRepository<TwinProfile, String> {

    // Custom query to find a TwinProfile by userId
    Optional<TwinProfile> findByUserId(String userId);

    long deleteByUserId(String userId);
}
