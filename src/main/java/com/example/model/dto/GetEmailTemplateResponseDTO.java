package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetEmailTemplateResponseDTO {
    @JsonProperty("id")
    private String id;

    @Schema(description = "Template HTML file")
    @JsonProperty("template_html_file_name")
    private String templateHtmlFileName;

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

    @JsonProperty("reminder_day_interval")
    private Integer reminderDayInterval;

    @JsonProperty("is_perm")
    private Boolean isPerm;

    @JsonProperty("created_at")
    private ZonedDateTime createdAt;

    @JsonProperty("updated_at")
    private ZonedDateTime updatedAt;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;
}
