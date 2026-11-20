package com.hostelos.mess;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "food_feedback")
@Getter
@NoArgsConstructor
public class FoodFeedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "meal_id")
    private Meal meal;
    @Column(name = "resident_id", nullable = false)
    private Long residentId;
    @Column(name = "taste_rating", nullable = false)
    private int tasteRating;
    @Column(name = "hygiene_rating", nullable = false)
    private int hygieneRating;
    private String comment;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public FoodFeedback(Meal meal, Long residentId, int tasteRating, int hygieneRating, String comment, LocalDateTime createdAt) {
        this.meal = meal;
        this.residentId = residentId;
        this.tasteRating = tasteRating;
        this.hygieneRating = hygieneRating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public void revise(int tasteRating, int hygieneRating, String comment, LocalDateTime now) {
        this.tasteRating = tasteRating;
        this.hygieneRating = hygieneRating;
        this.comment = comment;
        this.createdAt = now;
    }
}
