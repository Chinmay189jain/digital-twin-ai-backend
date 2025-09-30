package com.digitaltwin.backend.bootstrap;

import com.digitaltwin.backend.model.ProfileQuestion;
import com.digitaltwin.backend.repository.ProfileQuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProfileQuestionSeeder implements CommandLineRunner {

    private final ProfileQuestionRepository profileQuestionRepository;
    private final ObjectMapper mapper;

    @Override
    public void run(String... args) throws Exception {
        try (InputStream is = new ClassPathResource("profile-questions.json").getInputStream()) {
            List<ProfileQuestion> questions = mapper.readValue(is, new TypeReference<>() {});
            questions.forEach(profileQuestionRepository::save); // overwrites docs with same @Id
        }
    }
}
