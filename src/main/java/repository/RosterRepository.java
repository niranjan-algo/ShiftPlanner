package com.shiftplanner.repository;

import com.shiftplanner.entity.Roster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RosterRepository extends JpaRepository<Roster, Long> {
}