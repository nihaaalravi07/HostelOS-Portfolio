package com.hostelos.accommodation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hostels")
@Getter
@NoArgsConstructor
public class Hostel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    private String description;

    public Hostel(String name, String description) { this.name = name; this.description = description; }
}
