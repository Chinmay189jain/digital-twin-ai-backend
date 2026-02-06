package com.digitaltwin.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatSessionListItem {

    private String id;
    private String title;
    private Integer messageCount;
    private LocalDateTime updatedAt;
}
