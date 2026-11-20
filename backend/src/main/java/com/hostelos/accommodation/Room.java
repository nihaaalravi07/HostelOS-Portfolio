package com.hostelos.accommodation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "hostel_id")
    private Hostel hostel;
    @Column(name = "floor_no", nullable = false)
    private int floorNo;
    @Column(name = "room_number", nullable = false)
    private String roomNumber;
    @Column(nullable = false)
    private int capacity;

    public Room(Hostel hostel, int floorNo, String roomNumber, int capacity) {
        this.hostel = hostel;
        this.floorNo = floorNo;
        this.roomNumber = roomNumber;
        this.capacity = capacity;
    }

    public String label() { return hostel.getName() + " · Room " + roomNumber; }
}
