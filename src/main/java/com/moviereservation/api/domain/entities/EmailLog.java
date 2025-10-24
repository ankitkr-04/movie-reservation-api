package com.moviereservation.api.domain.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.moviereservation.api.domain.enums.EmailStatus;
import com.moviereservation.api.domain.enums.EmailType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "email_logs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@ToString(exclude = {"emailBody"})
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "email_log_id")
    private UUID id;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false)
    private EmailType emailType;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmailStatus status = EmailStatus.PENDING;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Short retryCount = 0;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "email_body", columnDefinition = "TEXT")
    private String emailBody;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmailLog)) return false;
        EmailLog emailLog = (EmailLog) o;
        return id != null && Objects.equals(id, emailLog.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}