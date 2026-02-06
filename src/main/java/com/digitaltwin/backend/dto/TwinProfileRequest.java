package com.digitaltwin.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwinProfileRequest {

    @NotNull(message = "profileAnswers is required")
    @NotEmpty(message = "profileAnswers cannot be empty")
    @Valid
    private Map<Integer,
            @NotBlank(message = "Answer cannot be blank")
            @Size(max = 2000, message = "Answer must be at most 2000 characters")
                    String> profileAnswers;
}
