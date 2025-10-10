package com.project.airhotel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @Column(nullable = false, unique = true)
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
}
