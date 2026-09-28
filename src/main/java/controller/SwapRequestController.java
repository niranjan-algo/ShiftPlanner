package com.shiftplanner.controller;

import com.shiftplanner.entity.SwapRequest;
import com.shiftplanner.service.SwapRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/swap-requests")
public class SwapRequestController {

    private final SwapRequestService swapRequestService;

    @Autowired
    public SwapRequestController(SwapRequestService swapRequestService) {
        this.swapRequestService = swapRequestService;
    }

    @GetMapping
    public ResponseEntity<List<SwapRequest>> getAllSwapRequests() {
        return ResponseEntity.ok(swapRequestService.getAllSwapRequests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SwapRequest> getSwapRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(swapRequestService.getSwapRequestById(id));
    }

    @PostMapping
    public ResponseEntity<SwapRequest> createSwapRequest(@RequestBody SwapRequest request) {
        SwapRequest created = swapRequestService.createSwapRequest(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/colleague-response")
    public ResponseEntity<SwapRequest> colleagueResponse(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String action = payload.get("action");
        SwapRequest updated = swapRequestService.colleagueRespond(id, action);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/manager-decision")
    public ResponseEntity<SwapRequest> managerDecision(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String decision = payload.get("decision");
        String notes = payload.get("notes");
        SwapRequest updated = swapRequestService.managerDecision(id, decision, notes);
        return ResponseEntity.ok(updated);
    }
}
