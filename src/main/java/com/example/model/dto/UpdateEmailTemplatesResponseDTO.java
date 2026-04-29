package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateEmailTemplatesResponseDTO {
    private String id;
    private String templateName;
    private String subject;
    private String mainBody;
    private String importantInfoIntro;
    private String importantInfoBody;
    private String contactBody;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
