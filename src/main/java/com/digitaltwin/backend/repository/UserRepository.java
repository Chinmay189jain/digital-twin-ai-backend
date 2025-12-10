package com.digitaltwin.backend.repository;

import com.digitaltwin.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    // Custom query to find a user by email
    Optional<User> findByEmail(String email);

    void deleteByEmail(String email);
}
