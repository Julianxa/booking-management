package com.example.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@Builder
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "email_templates")
public class EmailTemplates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;
    @Column(name = "ref_no", nullable = false)
    private String refNo;
    @Column(name = "template_name")
    private String templateName;
    @Column(name = "title")
    private String title;
    @Column(name = "title_zh_cn")
    private String titleZhCn;
    @Column(name = "title_zh_hk")
    private String titleZhHk;
    @Column(name = "subject")
    private String subject;
    @Column(name = "subject_zh_cn")
    private String subjectZhCn;
    @Column(name = "subject_zh_hk")
    private String subjectZhHk;
    @Column(name = "main_body")
    private String mainBody;
    @Column(name = "main_body_zh_cn")
    private String mainBodyZhCn;
    @Column(name = "main_body_zh_hk")
    private String mainBodyZhHk;
    @Column(name = "important_info_intro")
    private String importantInfoIntro;
    @Column(name = "important_info_intro_zh_cn")
    private String importantInfoIntroZhCn;
    @Column(name = "important_info_intro_zh_hk")
    private String importantInfoIntroZhHk;
    @Column(name = "important_info_body")
    private String importantInfoBody;
    @Column(name = "important_info_body_zh_cn")
    private String importantInfoBodyZhCn;
    @Column(name = "important_info_body_zh_hk")
    private String importantInfoBodyZhHk;
    @Column(name = "contact_body")
    private String contactBody;
    @Column(name = "contact_body_zh_cn")
    private String contactBodyZhCn;
    @Column(name = "contact_body_zh_hk")
    private String contactBodyZhHk;
    @Column(name = "is_perm")
    private Boolean isPerm;
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }
}
