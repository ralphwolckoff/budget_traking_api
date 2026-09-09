package com.budgettracker.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "forecast_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForecastItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String monthKey;

    private String catId;

    private String label;

    @Column(nullable = false)
    private Integer price;

    @Builder.Default
    @Column(nullable = false)
    private boolean done = false;
}
