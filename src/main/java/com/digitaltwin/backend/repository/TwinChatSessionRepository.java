package com.digitaltwin.backend.repository;

import com.digitaltwin.backend.model.TwinChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface TwinChatSessionRepository extends MongoRepository<TwinChatSession, String> {

    List<TwinChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);

    @Query("{ 'userId': ?0, 'title': { $regex: ?1, $options: 'i' } }")
    List<TwinChatSession> searchByTitle(String userId, String q);

    long deleteByUserId(String userId);
}
