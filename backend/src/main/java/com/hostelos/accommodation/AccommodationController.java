package com.hostelos.accommodation;

import com.hostelos.accommodation.AccommodationService.*;
import com.hostelos.resident.ResidentService;
import com.hostelos.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accommodation")
public class AccommodationController {
    public record AddRoomRequest(@NotNull Long hostelId, @Min(0) int floorNo, @NotBlank String roomNumber,
                                 @Min(1) @Max(6) int capacity) {}
    public record AllocateRequest(@NotNull Long residentId, @NotNull Long bedId) {}
    public record ReleaseRequest(@NotNull Long residentId) {}

    private final AccommodationService service;
    private final ResidentService residents;

    public AccommodationController(AccommodationService service, ResidentService residents) {
        this.service = service;
        this.residents = residents;
    }

    /** A resident may only see their own accommodation. */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public AccommodationInfo mine() {
        return service.requireCurrent(residents.requireCurrent().getId());
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public List<HostelView> overview() { return service.overview(); }

    @GetMapping("/occupancy")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public Occupancy occupancy() { return service.occupancy(); }

    @GetMapping("/residents")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public List<ResidentRow> residentDirectory() { return service.residentDirectory(); }

    @GetMapping("/recommend")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public List<BedSuggestion> recommend(@RequestParam Long residentId, @RequestParam(required = false) String strategy) {
        return service.recommend(residentId, strategy);
    }

    @PostMapping("/rooms")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public RoomView addRoom(@Valid @RequestBody AddRoomRequest r) {
        return service.addRoom(r.hostelId(), r.floorNo(), r.roomNumber().trim(), r.capacity());
    }

    @PostMapping("/allocate")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public AccommodationInfo allocate(@Valid @RequestBody AllocateRequest r) { return service.allocate(r.residentId(), r.bedId()); }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public AccommodationInfo transfer(@Valid @RequestBody AllocateRequest r) { return service.transfer(r.residentId(), r.bedId()); }

    @PostMapping("/release")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public void release(@Valid @RequestBody ReleaseRequest r) { service.release(r.residentId()); }
}
