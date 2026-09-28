package com.shiftplanner.controller;

import com.shiftplanner.entity.Roster;
import com.shiftplanner.service.RosterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rosters")
public class RosterController {

    private final RosterService rosterService;

    @Autowired
    public RosterController(RosterService rosterService) {
        this.rosterService = rosterService;
    }

    @GetMapping
    public ResponseEntity<List<Roster>> getAllRosters() {
        return ResponseEntity.ok(rosterService.getAllRosters());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Roster> getRosterById(@PathVariable Long id) {
        return ResponseEntity.ok(rosterService.getRosterById(id));
    }

    @PostMapping
    public ResponseEntity<Roster> createRoster(@RequestBody Roster roster) {
        Roster created = rosterService.createRoster(roster);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
}