package com.shiftplanner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "swap_requests")
public class SwapRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long requesterId;

    @Column(nullable = false)
    private Long colleagueId;

    @Column(nullable = false)
    private Long shiftId;

    private String shiftDate;
    private String shiftTime;
    private String shiftType;
    private String reason;
    private String requestedAt;

    private String status = "PENDING_COLLEAGUE"; // PENDING_COLLEAGUE, AWAITING_MANAGER, APPROVED, REJECTED, DECLINED

    private String colleagueResponse;
    private String colleagueResponseAt;
    private String managerDecision;
    private String managerDecisionAt;
    private String managerNotes;

    public SwapRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public Long getColleagueId() {
        return colleagueId;
    }

    public void setColleagueId(Long colleagueId) {
        this.colleagueId = colleagueId;
    }

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    public String getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(String shiftDate) {
        this.shiftDate = shiftDate;
    }

    public String getShiftTime() {
        return shiftTime;
    }

    public void setShiftTime(String shiftTime) {
        this.shiftTime = shiftTime;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(String requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getColleagueResponse() {
        return colleagueResponse;
    }

    public void setColleagueResponse(String colleagueResponse) {
        this.colleagueResponse = colleagueResponse;
    }

    public String getColleagueResponseAt() {
        return colleagueResponseAt;
    }

    public void setColleagueResponseAt(String colleagueResponseAt) {
        this.colleagueResponseAt = colleagueResponseAt;
    }

    public String getManagerDecision() {
        return managerDecision;
    }

    public void setManagerDecision(String managerDecision) {
        this.managerDecision = managerDecision;
    }

    public String getManagerDecisionAt() {
        return managerDecisionAt;
    }

    public void setManagerDecisionAt(String managerDecisionAt) {
        this.managerDecisionAt = managerDecisionAt;
    }

    public String getManagerNotes() {
        return managerNotes;
    }

    public void setManagerNotes(String managerNotes) {
        this.managerNotes = managerNotes;
    }
}
