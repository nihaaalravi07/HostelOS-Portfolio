package com.hostelos.mess;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meals")
@Getter
@NoArgsConstructor
public class Meal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MealCategory category;
    @Column(name = "menu_items", nullable = false)
    private String menuItems;                    // comma-separated
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(name = "cutoff_time", nullable = false)
    private LocalDateTime cutoffTime;
    private boolean available = true;
    @Column(name = "image_url")
    private String imageUrl;

    public Meal(LocalDate mealDate, MealCategory category, String menuItems, BigDecimal price,
                LocalDateTime cutoffTime, String imageUrl) {
        this.mealDate = mealDate;
        this.category = category;
        this.menuItems = menuItems;
        this.price = price;
        this.cutoffTime = cutoffTime;
        this.imageUrl = imageUrl;
    }

    public List<String> menuList() {
        return Arrays.stream(menuItems.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public boolean isBookingOpen(LocalDateTime now) { return available && now.isBefore(cutoffTime); }

    public LocalDateTime startsAt() { return mealDate.atTime(category.getStart()); }

    public LocalDateTime endsAt() { return mealDate.atTime(category.getEnd()); }

    /** Menu manager edits. Already-billed meals keep the price they were charged (stored on the booking). */
    public void update(String menuItems, BigDecimal price, LocalDateTime cutoffTime, boolean available) {
        this.menuItems = menuItems;
        this.price = price;
        this.cutoffTime = cutoffTime;
        this.available = available;
    }
}
