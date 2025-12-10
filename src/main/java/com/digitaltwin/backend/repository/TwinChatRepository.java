package com.digitaltwin.backend.repository;

import com.digitaltwin.backend.model.TwinChat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TwinChatRepository extends MongoRepository<TwinChat, String> {

    // Custom query to find a TwinChat by userId and sessionId, ordered by timestamp descending
    Slice<TwinChat> findByUserIdAndSessionIdOrderByTimestampAsc(String userId, String sessionId, Pageable pageable);

    void deleteByUserIdAndSessionId(String userId, String sessionId);

    long deleteByUserId(String userId);
}
