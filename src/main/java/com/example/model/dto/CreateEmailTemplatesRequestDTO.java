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
public class CreateEmailTemplatesRequestDTO {
    @JsonProperty("template_name")
    private String templateName;

    @JsonProperty("title")
    private String title;

    @JsonProperty("title_zh_cn")
    private String titleZhCn;

    @JsonProperty("title_zh_hk")
    private String titleZhHk;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("subject_zh_cn")
    private String subjectZhCn;

    @JsonProperty("subject_zh_hk")
    private String subjectZhHk;

    @JsonProperty("main_body")
    private String mainBody;

    @JsonProperty("main_body_zh_cn")
    private String mainBodyZhCn;

    @JsonProperty("main_body_zh_hk")
    private String mainBodyZhHk;

    @JsonProperty("important_info_intro")
    private String importantInfoIntro;

    @JsonProperty("important_info_intro_zh_cn")
    private String importantInfoIntroZhCn;

    @JsonProperty("important_info_intro_zh_hk")
    private String importantInfoIntroZhHk;

    @JsonProperty("important_info_body")
    private String importantInfoBody;

    @JsonProperty("important_info_body_zh_cn")
    private String importantInfoBodyZhCn;

    @JsonProperty("important_info_body_zh_hk")
    private String importantInfoBodyZhHk;

    @JsonProperty("contact_body")
    private String contactBody;

    @JsonProperty("contact_body_zh_cn")
    private String contactBodyZhCn;

    @JsonProperty("contact_body_zh_hk")
    private String contactBodyZhHk;
}
