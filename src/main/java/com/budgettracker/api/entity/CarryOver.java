package com.budgettracker.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "carry_overs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarryOver {

    @EmbeddedId
    private CarryOverId id;

    @Column(nullable = false)
    private Integer amount;
}
