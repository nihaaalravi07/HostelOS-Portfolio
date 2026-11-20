package com.hostelos.accommodation;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A resident's stay in a bed. Ended allocations are kept as history (end_date set). */
@Entity
@Table(name = "allocations")
@Getter
@NoArgsConstructor
public class Allocation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "resident_id", nullable = false)
    private Long residentId;
    @ManyToOne(optional = false)
    @JoinColumn(name = "bed_id")
    private Bed bed;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;

    public Allocation(Long residentId, Bed bed, LocalDate startDate) {
        this.residentId = residentId;
        this.bed = bed;
        this.startDate = startDate;
    }

    public boolean isActive() { return endDate == null; }

    public void end(LocalDate date) { this.endDate = date; }
}
