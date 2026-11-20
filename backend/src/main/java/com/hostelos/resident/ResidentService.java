package com.hostelos.resident;

import com.hostelos.common.BusinessException;
import com.hostelos.security.CurrentUser;
import com.hostelos.user.User;
import com.hostelos.user.UserService;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ResidentService {
    private final ResidentRepository residents;
    private final UserService users;

    public ResidentService(ResidentRepository residents, UserService users) {
        this.residents = residents;
        this.users = users;
    }

    public Resident requireById(Long id) {
        return residents.findById(id).orElseThrow(() -> BusinessException.notFound("Resident not found"));
    }

    /** The resident profile of the signed-in student. */
    public Resident requireCurrent() {
        return residents.findByUserId(CurrentUser.get().userId())
                .orElseThrow(() -> BusinessException.forbidden("Only hostel residents can do this"));
    }

    public ResidentSummary summary(Long residentId) {
        return summaries(List.of(residentId)).get(residentId);
    }

    public Map<Long, ResidentSummary> summaries(Collection<Long> ids) {
        Set<Long> unique = new HashSet<>(ids);
        if (unique.isEmpty()) return Map.of();
        List<Resident> found = residents.findAllById(unique);
        Map<Long, User> userMap = new HashMap<>();
        for (Long uid : found.stream().map(Resident::getUserId).toList()) userMap.put(uid, users.require(uid));
        return found.stream().collect(Collectors.toMap(Resident::getId, r -> toSummary(r, userMap.get(r.getUserId()))));
    }

    public List<ResidentSummary> listAll() {
        List<Resident> all = residents.findAll();
        Map<Long, ResidentSummary> map = summaries(all.stream().map(Resident::getId).toList());
        return all.stream().map(r -> map.get(r.getId()))
                .sorted(Comparator.comparing(ResidentSummary::fullName)).toList();
    }

    public long countActive() { return residents.countByStatus(ResidentStatus.ACTIVE); }

    @Transactional
    public Resident register(User user, String enrollmentNo, String phone, String course, int year) {
        return residents.save(new Resident(user.getId(), enrollmentNo, phone, course, year));
    }

    @Transactional
    public void markCheckedOut(Long residentId) { requireById(residentId).checkOut(); }

    private ResidentSummary toSummary(Resident r, User u) {
        return new ResidentSummary(r.getId(), r.getUserId(), u.getFullName(), u.getEmail(), r.getEnrollmentNo(),
                r.getCourse(), r.getYearOfStudy(), r.getPhone(), r.getStatus());
    }
}
