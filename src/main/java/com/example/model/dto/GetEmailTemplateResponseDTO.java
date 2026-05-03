package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class GetEmailTemplateResponseDTO {
    @JsonProperty("id")
    private String id;

    @JsonProperty("template_name")
    private String templateName;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("main_body")
    private String mainBody;

    @JsonProperty("important_info_intro")
    private String importantInfoIntro;

    @JsonProperty("important_info_body")
    private String importantInfoBody;

    @JsonProperty("contact_body")
    private String contactBody;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
