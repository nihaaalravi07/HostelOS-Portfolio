package com.hostelos.housekeeping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hostelos.common.BusinessException;
import com.hostelos.maintenance.Complaint;
import com.hostelos.maintenance.ComplaintCategory;
import com.hostelos.maintenance.ComplaintPriority;
import com.hostelos.maintenance.ComplaintStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StatusTransitionTest {
    private CleaningRequest cleaning() {
        return new CleaningRequest(1L, 1L, "Room 101", LocalDate.now().plusDays(1), CleaningSlot.MORNING, null);
    }

    private Complaint complaint() {
        return new Complaint(1L, 1L, "Room 101", ComplaintCategory.PLUMBING, ComplaintPriority.HIGH, "Leak");
    }

    @Test
    void cleaningFollowsTheHappyPath() {
        CleaningRequest r = cleaning();
        r.moveTo(CleaningStatus.SCHEDULED);
        r.assignTo(9L);
        r.moveTo(CleaningStatus.IN_PROGRESS);
        r.complete("done");
        assertThat(r.getStatus()).isEqualTo(CleaningStatus.COMPLETED);
        assertThat(r.getCompletionNotes()).isEqualTo("done");
    }

    @Test
    void cleaningCannotSkipSteps() {
        CleaningRequest r = cleaning();
        r.moveTo(CleaningStatus.SCHEDULED);
        assertThatThrownBy(() -> r.complete("x")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> r.moveTo(CleaningStatus.IN_PROGRESS)).isInstanceOf(BusinessException.class);
    }

    @Test
    void completedOrCancelledCleaningIsFinal() {
        CleaningRequest r = cleaning();
        r.moveTo(CleaningStatus.CANCELLED);
        assertThat(CleaningStatus.CANCELLED.allowedNext()).isEmpty();
        assertThatThrownBy(() -> r.moveTo(CleaningStatus.SCHEDULED)).isInstanceOf(BusinessException.class);
    }

    @Test
    void complaintFollowsTheHappyPath() {
        Complaint c = complaint();
        c.assignTo(5L);
        c.start();
        c.resolve("Replaced washer");
        c.close();
        assertThat(c.getStatus()).isEqualTo(ComplaintStatus.CLOSED);
    }

    @Test
    void complaintCannotBeResolvedWithoutAssignmentOrNotes() {
        Complaint c = complaint();
        assertThatThrownBy(() -> c.resolve("x")).isInstanceOf(BusinessException.class);   // OPEN -> RESOLVED not allowed
        c.assignTo(5L);
        c.start();
        assertThatThrownBy(() -> c.resolve("  ")).isInstanceOf(BusinessException.class).hasMessageContaining("what was done");
    }

    @Test
    void resolvedComplaintCanBeReopenedButClosedCannot() {
        Complaint c = complaint();
        c.assignTo(5L);
        c.start();
        c.resolve("Tightened");
        c.start();                                                  // fix did not hold
        assertThat(c.getStatus()).isEqualTo(ComplaintStatus.IN_PROGRESS);
        c.resolve("Replaced the part");
        c.close();
        assertThatThrownBy(c::start).isInstanceOf(BusinessException.class);
    }
}
