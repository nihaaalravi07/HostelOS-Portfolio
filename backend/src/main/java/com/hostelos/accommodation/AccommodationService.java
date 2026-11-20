package com.hostelos.accommodation;

import com.hostelos.accommodation.AllocationStrategy.FreeBed;
import com.hostelos.common.BusinessException;
import com.hostelos.resident.Resident;
import com.hostelos.resident.ResidentService;
import com.hostelos.resident.ResidentSummary;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccommodationService {
    public record BedView(Long id, int bedNumber, Long residentId, String residentName) {}
    public record RoomView(Long id, String roomNumber, int floorNo, int capacity, int occupied, List<BedView> beds) {}
    public record HostelView(Long id, String name, int totalBeds, int occupiedBeds, List<RoomView> rooms) {}
    public record Occupancy(int totalBeds, int occupiedBeds, int availableBeds) {}
    public record BedSuggestion(Long bedId, String label, int occupantsInRoom, String reason) {}
    public record ResidentRow(ResidentSummary resident, AccommodationInfo accommodation) {}

    private final HostelRepository hostels;
    private final RoomRepository rooms;
    private final BedRepository beds;
    private final AllocationRepository allocations;
    private final ResidentService residents;
    private final Map<String, AllocationStrategy> strategies;

    public AccommodationService(HostelRepository hostels, RoomRepository rooms, BedRepository beds,
                                AllocationRepository allocations, ResidentService residents,
                                List<AllocationStrategy> strategyBeans) {
        this.hostels = hostels;
        this.rooms = rooms;
        this.beds = beds;
        this.allocations = allocations;
        this.residents = residents;
        this.strategies = strategyBeans.stream().collect(Collectors.toMap(AllocationStrategy::name, s -> s));
    }

    // ------------------------------------------------------------------ queries

    public List<HostelView> overview() {
        Map<Long, Allocation> byBed = allocations.findByEndDateIsNull().stream()
                .collect(Collectors.toMap(a -> a.getBed().getId(), a -> a));
        Map<Long, ResidentSummary> names = residents.summaries(
                byBed.values().stream().map(Allocation::getResidentId).toList());
        List<HostelView> result = new ArrayList<>();
        for (Hostel h : hostels.findAll()) {
            List<RoomView> roomViews = new ArrayList<>();
            int total = 0, occupied = 0;
            for (Room r : rooms.findByHostelIdOrderByFloorNoAscRoomNumberAsc(h.getId())) {
                List<BedView> bedViews = new ArrayList<>();
                int occ = 0;
                for (Bed b : beds.findByRoomIdOrderByBedNumber(r.getId())) {
                    Allocation a = byBed.get(b.getId());
                    if (a != null) occ++;
                    bedViews.add(new BedView(b.getId(), b.getBedNumber(),
                            a == null ? null : a.getResidentId(),
                            a == null ? null : names.get(a.getResidentId()).fullName()));
                }
                total += bedViews.size();
                occupied += occ;
                roomViews.add(new RoomView(r.getId(), r.getRoomNumber(), r.getFloorNo(), r.getCapacity(), occ, bedViews));
            }
            result.add(new HostelView(h.getId(), h.getName(), total, occupied, roomViews));
        }
        return result;
    }

    public Occupancy occupancy() {
        int total = (int) beds.count();
        int occupied = allocations.findByEndDateIsNull().size();
        return new Occupancy(total, occupied, total - occupied);
    }

    public Optional<AccommodationInfo> currentFor(Long residentId) {
        return allocations.findByResidentIdAndEndDateIsNull(residentId).map(this::toInfo);
    }

    public AccommodationInfo requireCurrent(Long residentId) {
        return currentFor(residentId)
                .orElseThrow(() -> BusinessException.conflict("You have not been allocated a bed yet"));
    }

    public List<ResidentRow> residentDirectory() {
        Map<Long, Allocation> active = allocations.findByEndDateIsNull().stream()
                .collect(Collectors.toMap(Allocation::getResidentId, a -> a));
        return residents.listAll().stream()
                .map(s -> new ResidentRow(s, active.containsKey(s.id()) ? toInfo(active.get(s.id())) : null)).toList();
    }

    public List<BedSuggestion> recommend(Long residentId, String strategyName) {
        residents.requireById(residentId);
        AllocationStrategy strategy = strategies.get(strategyName == null ? "PACK" : strategyName.toUpperCase());
        if (strategy == null) throw BusinessException.badRequest("Unknown strategy. Use " + strategies.keySet());
        return strategy.rank(freeBeds()).stream().limit(5).map(f -> new BedSuggestion(f.bed().getId(),
                f.bed().getRoom().label() + " · Bed " + f.bed().getBedNumber(), f.occupantsInRoom(),
                strategy.name().equals("PACK")
                        ? (f.occupantsInRoom() > 0 ? "Fills a partly occupied room" : "Opens a new room")
                        : "Roomier option with " + f.occupantsInRoom() + " current occupant(s)")).toList();
    }

    // ------------------------------------------------------------------ commands

    @Transactional
    public Hostel createHostel(String name, String description) { return hostels.save(new Hostel(name, description)); }

    @Transactional
    public RoomView addRoom(Long hostelId, int floorNo, String roomNumber, int capacity) {
        Hostel hostel = hostels.findById(hostelId).orElseThrow(() -> BusinessException.notFound("Hostel not found"));
        if (rooms.existsByHostelIdAndRoomNumber(hostelId, roomNumber))
            throw BusinessException.conflict("Room " + roomNumber + " already exists in " + hostel.getName());
        Room room = rooms.save(new Room(hostel, floorNo, roomNumber, capacity));
        List<BedView> bedViews = new ArrayList<>();
        for (int i = 1; i <= capacity; i++) {
            Bed bed = beds.save(new Bed(room, i));
            bedViews.add(new BedView(bed.getId(), i, null, null));
        }
        return new RoomView(room.getId(), roomNumber, floorNo, capacity, 0, bedViews);
    }

    @Transactional
    public AccommodationInfo allocate(Long residentId, Long bedId) {
        Resident resident = residents.requireById(residentId);
        if (!resident.isActive()) throw BusinessException.conflict("Resident has checked out");
        if (allocations.findByResidentIdAndEndDateIsNull(residentId).isPresent())
            throw BusinessException.conflict("Resident already has a bed. Use transfer instead.");
        return assign(residentId, bedId);
    }

    @Transactional
    public AccommodationInfo transfer(Long residentId, Long bedId) {
        Allocation current = allocations.findByResidentIdAndEndDateIsNull(residentId)
                .orElseThrow(() -> BusinessException.conflict("Resident has no current bed to transfer from"));
        if (current.getBed().getId().equals(bedId)) throw BusinessException.badRequest("Resident is already in that bed");
        Bed target = beds.findByIdForUpdate(bedId).orElseThrow(() -> BusinessException.notFound("Bed not found"));
        if (allocations.existsByBedIdAndEndDateIsNull(target.getId()))
            throw BusinessException.conflict("That bed is already occupied");
        current.end(LocalDate.now());
        allocations.saveAndFlush(current);          // free the old bed before taking the new one
        return assign(residentId, bedId);
    }

    @Transactional
    public void release(Long residentId) {
        Allocation current = allocations.findByResidentIdAndEndDateIsNull(residentId)
                .orElseThrow(() -> BusinessException.conflict("Resident has no active allocation"));
        current.end(LocalDate.now());
    }

    // ------------------------------------------------------------------ helpers

    private AccommodationInfo assign(Long residentId, Long bedId) {
        Bed bed = beds.findByIdForUpdate(bedId).orElseThrow(() -> BusinessException.notFound("Bed not found"));
        if (allocations.existsByBedIdAndEndDateIsNull(bed.getId()))
            throw BusinessException.conflict("That bed is already occupied");
        try {
            Allocation saved = allocations.saveAndFlush(new Allocation(residentId, bed, LocalDate.now()));
            return toInfo(saved);
        } catch (DataIntegrityViolationException e) {   // partial unique index caught a race
            throw BusinessException.conflict("That bed is already occupied");
        }
    }

    private List<FreeBed> freeBeds() {
        Set<Long> taken = allocations.findByEndDateIsNull().stream().map(a -> a.getBed().getId()).collect(Collectors.toSet());
        Map<Long, Integer> occupantsByRoom = new HashMap<>();
        allocations.findByEndDateIsNull().forEach(a -> occupantsByRoom.merge(a.getBed().getRoom().getId(), 1, Integer::sum));
        return beds.findAll().stream().filter(b -> !taken.contains(b.getId()))
                .map(b -> new FreeBed(b, occupantsByRoom.getOrDefault(b.getRoom().getId(), 0))).toList();
    }

    private AccommodationInfo toInfo(Allocation a) {
        Room room = a.getBed().getRoom();
        List<Long> mateIds = allocations.findByBedRoomIdAndEndDateIsNull(room.getId()).stream()
                .map(Allocation::getResidentId).filter(id -> !id.equals(a.getResidentId())).toList();
        List<String> mates = residents.summaries(mateIds).values().stream().map(ResidentSummary::fullName).sorted().toList();
        return new AccommodationInfo(a.getId(), room.getHostel().getId(), room.getHostel().getName(), room.getId(),
                room.getRoomNumber(), room.getFloorNo(), a.getBed().getBedNumber(), room.getCapacity(),
                a.getStartDate(), mates);
    }
}
