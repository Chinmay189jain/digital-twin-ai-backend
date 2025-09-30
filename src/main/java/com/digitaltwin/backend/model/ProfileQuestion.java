package com.digitaltwin.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("profile_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileQuestion {

    @Id
    private Integer id;            // keep same ids as your TS array
    private String prefix;
    private String question;
    private QuestionType type;     // "select", "radio", "text"
    private List<String> options;  // null for "text"
    private String placeholder;
}
