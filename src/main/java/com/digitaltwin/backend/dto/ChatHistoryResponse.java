package com.digitaltwin.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ChatHistoryResponse {

    private List<TwinAnswerResponse> messages;
    private boolean hasMore;
}
