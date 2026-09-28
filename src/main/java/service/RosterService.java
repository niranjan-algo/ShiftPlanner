package com.shiftplanner.service;

import com.shiftplanner.entity.Roster;
import com.shiftplanner.exception.ResourceNotFoundException;
import com.shiftplanner.repository.RosterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RosterService {

    private final RosterRepository rosterRepository;

    @Autowired
    public RosterService(RosterRepository rosterRepository) {
        this.rosterRepository = rosterRepository;
    }

    public List<Roster> getAllRosters() {
        return rosterRepository.findAll();
    }

    public Roster getRosterById(Long id) {
        return rosterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Roster not found with id: " + id));
    }

    public Roster createRoster(Roster roster) {
        if (roster.getStatus() == null || roster.getStatus().trim().isEmpty()) {
            roster.setStatus("PUBLISHED");
        }
        return rosterRepository.save(roster);
    }
}