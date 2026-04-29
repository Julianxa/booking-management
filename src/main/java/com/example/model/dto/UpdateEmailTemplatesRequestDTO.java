package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmailTemplatesRequestDTO {
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
}
