package com.hostelos.finance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    Optional<LedgerEntry> findBySourceModuleAndSourceRef(String sourceModule, String sourceRef);

    List<LedgerEntry> findByResidentIdOrderByCreatedAtAscIdAsc(Long residentId);

    @Query("select e.type, coalesce(sum(e.amount), 0) from LedgerEntry e where e.residentId = :rid group by e.type")
    List<Object[]> totalsByType(@Param("rid") Long residentId);

    @Query("select e.residentId, e.type, sum(e.amount) from LedgerEntry e group by e.residentId, e.type")
    List<Object[]> totalsByResidentAndType();

    @Query("""
           select e.type, coalesce(sum(e.amount), 0) from LedgerEntry e
           where e.residentId = :rid and e.sourceModule = :module and e.createdAt >= :from and e.createdAt < :to
           group by e.type""")
    List<Object[]> moduleTotals(@Param("rid") Long residentId, @Param("module") String module,
                                @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
