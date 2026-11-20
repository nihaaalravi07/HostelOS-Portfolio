package com.hostelos.accommodation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "beds")
@Getter
@NoArgsConstructor
public class Bed {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "room_id")
    private Room room;
    @Column(name = "bed_number", nullable = false)
    private int bedNumber;

    public Bed(Room room, int bedNumber) { this.room = room; this.bedNumber = bedNumber; }
}
