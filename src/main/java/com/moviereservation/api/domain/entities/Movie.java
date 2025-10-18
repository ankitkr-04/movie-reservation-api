package com.moviereservation.api.domain.entities;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.moviereservation.api.domain.enums.Genre;
import com.moviereservation.api.domain.enums.MovieStatus;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "movies")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE movies SET deleted_at = CURRENT_TIMESTAMP WHERE movie_id = ?")
@SQLRestriction("deleted_at IS NULL")
@Data
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "movie_id")
    private UUID id;

    @Column(name = "title", nullable = false, unique = true)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration", nullable = false)
    private Integer duration; // minutes

    @Enumerated(EnumType.STRING)
    @Column(name = "genre", nullable = false)
    private Genre genre;

    @Column(name = "release_date")
    private Instant releaseDate;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "rating", length = 10)
    private String rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MovieStatus status = MovieStatus.ACTIVE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
