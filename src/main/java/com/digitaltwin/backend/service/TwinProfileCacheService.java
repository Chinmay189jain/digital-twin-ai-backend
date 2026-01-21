package com.digitaltwin.backend.service;

import com.digitaltwin.backend.dto.ProfileResponse;
import com.digitaltwin.backend.model.TwinProfile;
import com.digitaltwin.backend.repository.TwinProfileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class TwinProfileCacheService {

    @Autowired
    private TwinProfileRepository twinProfileRepository;

    private static final Logger logger = LoggerFactory.getLogger(TwinProfileCacheService.class);

    @Cacheable(cacheNames = "profileCache", key = "#userId")
    public ProfileResponse getProfile(String userId) {
        logger.info("CACHE MISS -> Loading profile from DB for userId={}", userId);
        TwinProfile profile = twinProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Twin profile not found"));

        return new ProfileResponse(profile.getProfileAnswers(), profile.getProfileSummary());
    }

    @CacheEvict(cacheNames = "profileCache", key = "#userId")
    public void evictProfile(String userId) {
        // no code needed
    }
}
