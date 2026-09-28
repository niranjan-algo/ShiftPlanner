package com.shiftplanner.service;

import com.shiftplanner.entity.Employee;
import com.shiftplanner.entity.Shift;
import com.shiftplanner.entity.SwapRequest;
import com.shiftplanner.exception.ResourceNotFoundException;
import com.shiftplanner.repository.EmployeeRepository;
import com.shiftplanner.repository.ShiftRepository;
import com.shiftplanner.repository.SwapRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SwapRequestService {

    private final SwapRequestRepository swapRequestRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    public SwapRequestService(SwapRequestRepository swapRequestRepository,
            ShiftRepository shiftRepository,
            EmployeeRepository employeeRepository) {
        this.swapRequestRepository = swapRequestRepository;
        this.shiftRepository = shiftRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<SwapRequest> getAllSwapRequests() {
        return swapRequestRepository.findAll();
    }

    public SwapRequest getSwapRequestById(Long id) {
        return swapRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Swap request not found with id: " + id));
    }

    @Transactional
    public SwapRequest createSwapRequest(SwapRequest request) {
        if (request.getRequesterId() == null || request.getColleagueId() == null || request.getShiftId() == null) {
            throw new IllegalArgumentException("Requester, colleague, and shift are required");
        }
        if (request.getRequesterId().equals(request.getColleagueId())) {
            throw new IllegalArgumentException("A shift cannot be requested from its current assignee");
        }
        Employee requester = employeeRepository.findById(request.getRequesterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Requester not found with id: " + request.getRequesterId()));
        employeeRepository.findById(request.getColleagueId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Colleague not found with id: " + request.getColleagueId()));
        Shift shift = shiftRepository.findByIdForUpdate(request.getShiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found with id: " + request.getShiftId()));
        if (!shift.getEmployeeId().equals(request.getRequesterId()) || !"ASSIGNED".equals(shift.getStatus())) {
            throw new IllegalArgumentException("Only an assigned shift owned by the requester can be offered");
        }
        if (swapRequestRepository.existsByShiftIdAndStatusIn(request.getShiftId(),
                List.of("PENDING_COLLEAGUE", "AWAITING_MANAGER"))) {
            throw new IllegalArgumentException("This shift already has an active swap request");
        }
        request.setStatus("PENDING_COLLEAGUE");
        request.setRequestedAt(LocalDateTime.now().format(FORMATTER));
        shift.setStatus("SWAP_PENDING");
        shiftRepository.save(shift);
        request.setShiftDate(shift.getDate());
        request.setShiftTime(shift.getStartTime() + " - " + shift.getEndTime());
        request.setShiftType(shift.getShiftType());
        request.setRequesterId(requester.getId());
        return swapRequestRepository.save(request);
    }

    @Transactional
    public SwapRequest colleagueRespond(Long id, String action) {
        SwapRequest request = getSwapRequestById(id);
        if (!"PENDING_COLLEAGUE".equals(request.getStatus())) {
            throw new IllegalArgumentException("This request is no longer waiting for a colleague response");
        }
        String nowStr = LocalDateTime.now().format(FORMATTER);

        if ("ACCEPT".equalsIgnoreCase(action)) {
            request.setColleagueResponse("ACCEPTED");
            request.setColleagueResponseAt(nowStr);
            request.setStatus("AWAITING_MANAGER");
        } else if ("DECLINE".equalsIgnoreCase(action)) {
            request.setColleagueResponse("DECLINED");
            request.setColleagueResponseAt(nowStr);
            request.setStatus("DECLINED");

            // Revert shift status
            Shift shift = shiftRepository.findByIdForUpdate(request.getShiftId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Shift not found with id: " + request.getShiftId()));
            shift.setStatus("ASSIGNED");
            shiftRepository.save(shift);
        } else {
            throw new IllegalArgumentException("Action must be ACCEPT or DECLINE");
        }

        return swapRequestRepository.save(request);
    }

    @Transactional
    public SwapRequest managerDecision(Long id, String decision, String notes) {
        SwapRequest request = getSwapRequestById(id);
        if (!"AWAITING_MANAGER".equals(request.getStatus())) {
            throw new IllegalArgumentException("This request is not waiting for manager approval");
        }
        String nowStr = LocalDateTime.now().format(FORMATTER);

        if ("APPROVE".equalsIgnoreCase(decision)) {
            request.setManagerDecision("APPROVED");
            request.setManagerDecisionAt(nowStr);
            request.setStatus("APPROVED");
            request.setManagerNotes(notes != null && !notes.isEmpty() ? notes : "Approved by Manager. Roster updated.");

            // Update live roster: reassign shift to colleague!
            Shift shift = shiftRepository.findByIdForUpdate(request.getShiftId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Shift not found with id: " + request.getShiftId()));
            if (!shift.getEmployeeId().equals(request.getRequesterId()) || !"SWAP_PENDING".equals(shift.getStatus())) {
                throw new IllegalArgumentException("The offered shift has changed and cannot be transferred");
            }
            for (Shift other : shiftRepository.findByEmployeeIdAndDate(request.getColleagueId(), shift.getDate())) {
                if (!other.getId().equals(shift.getId())
                        && isTimeOverlapping(other.getStartTime(), other.getEndTime(), shift.getStartTime(),
                                shift.getEndTime())) {
                    throw new IllegalArgumentException("The colleague already has an overlapping shift");
                }
            }
            Employee colleague = employeeRepository.findById(request.getColleagueId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Colleague not found with id: " + request.getColleagueId()));
            shift.setEmployeeId(colleague.getId());
            shift.setEmployeeName(colleague.getName());
            shift.setStatus("ASSIGNED");
            shiftRepository.save(shift);
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            request.setManagerDecision("REJECTED");
            request.setManagerDecisionAt(nowStr);
            request.setStatus("REJECTED");
            request.setManagerNotes(notes != null && !notes.isEmpty() ? notes : "Rejected by Manager.");

            // Revert shift status
            Shift shift = shiftRepository.findById(request.getShiftId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Shift not found with id: " + request.getShiftId()));
            shift.setStatus("ASSIGNED");
            shiftRepository.save(shift);
        } else {
            throw new IllegalArgumentException("Decision must be APPROVE or REJECT");
        }

        return swapRequestRepository.save(request);
    }

    private boolean isTimeOverlapping(String startA, String endA, String startB, String endB) {
        return startA.compareTo(endB) < 0 && endA.compareTo(startB) > 0;
    }
}
