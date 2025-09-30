package com.digitaltwin.backend.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ProfileAnswersRequest {

    private  Map<Integer, String> profileAnswers;
}
