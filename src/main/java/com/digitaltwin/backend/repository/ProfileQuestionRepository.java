package com.digitaltwin.backend.repository;

import com.digitaltwin.backend.model.ProfileQuestion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProfileQuestionRepository extends MongoRepository<ProfileQuestion, Integer> {

    List<ProfileQuestion> findAllByOrderByIdAsc();
}
