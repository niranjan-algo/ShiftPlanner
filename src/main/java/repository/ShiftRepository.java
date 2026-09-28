package com.shiftplanner.repository;

import com.shiftplanner.entity.Shift;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    List<Shift> findByEmployeeId(Long employeeId);

    List<Shift> findByDate(String date);

    List<Shift> findByEmployeeIdAndDate(Long employeeId, String date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shift from Shift shift where shift.id = :id")
    Optional<Shift> findByIdForUpdate(@Param("id") Long id);
}
