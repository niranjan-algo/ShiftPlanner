package com.shiftplanner.repository;

import com.shiftplanner.entity.SwapRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SwapRequestRepository extends JpaRepository<SwapRequest, Long> {
    List<SwapRequest> findByRequesterId(Long requesterId);

    List<SwapRequest> findByColleagueId(Long colleagueId);

    List<SwapRequest> findByStatus(String status);

    boolean existsByShiftIdAndStatusIn(Long shiftId, List<String> statuses);
}
