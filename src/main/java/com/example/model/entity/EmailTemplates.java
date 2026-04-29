package com.example.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import java.time.LocalDateTime;

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
    @Column(name = "template_name", nullable = false)
    private String templateName;
    @Column(name = "subject", nullable = false)
    private String subject;
    @Column(name = "main_body")
    private String mainBody;
    @Column(name = "important_info_intro")
    private String importantInfoIntro;
    @Column(name = "important_info_body")
    private String importantInfoBody;
    @Column(name = "contact_body")
    private String contactBody;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt   = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
