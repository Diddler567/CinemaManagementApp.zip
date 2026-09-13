package com.example.backend.repository;

import com.example.backend.entities.ScreeningReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScreeningReviewRepository extends JpaRepository<ScreeningReview, Long> {
    List<ScreeningReview> findByScreening_ScreeningID(Long screeningID);
    boolean existsByScreening_ScreeningIDAndStaff_UserID(Long screeningID, Long staffUserID);

    long countByScreening_ScreeningID(Long screeningID); 
}


