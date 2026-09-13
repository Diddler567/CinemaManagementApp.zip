package com.example.backend.repository;

import com.example.backend.entities.Screening;
import com.example.backend.entities.ScreeningState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    List<Screening> findByProgram_ProgramID(Long programID);

    @Query("""
           SELECT s
           FROM Screening s
           WHERE s.program.programID = :programID
             AND s.state = com.example.backend.entities.ScreeningState.SCHEDULED
             AND s.auditoriumName = :auditorium
             AND s.startTime < :endTime
             AND s.endTime > :startTime
           """)
    List<Screening> findOverlappingScheduled(
            @Param("programID") Long programID,
            @Param("auditorium") String auditorium,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
           SELECT s
           FROM Screening s
           WHERE s.program.programID = :programID
             AND s.state = com.example.backend.entities.ScreeningState.APPROVED
             AND s.finalSubmissionAt IS NULL
           """)
    List<Screening> findApprovedWithoutFinalSubmission(@Param("programID") Long programID);
}
