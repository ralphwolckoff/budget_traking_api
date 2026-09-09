package com.budgettracker.api.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CarryOverId implements Serializable {
    private String userId;
    private String monthKey;
}
