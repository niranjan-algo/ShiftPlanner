package com.shiftplanner.service;

import com.shiftplanner.entity.Employee;
import com.shiftplanner.entity.Shift;
import com.shiftplanner.exception.ResourceNotFoundException;
import com.shiftplanner.repository.EmployeeRepository;
import com.shiftplanner.repository.ShiftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.LocalDate;
import java.util.List;

@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    public ShiftService(ShiftRepository shiftRepository, EmployeeRepository employeeRepository) {
        this.shiftRepository = shiftRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<Shift> getAllShifts() {
        return shiftRepository.findAll();
    }

    public Shift getShiftById(Long id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found with id: " + id));
    }

    public List<Shift> getShiftsByEmployeeId(Long employeeId) {
        return shiftRepository.findByEmployeeId(employeeId);
    }

    public Shift createShift(Shift shift) {
        validateShift(shift);
        Employee employee = employeeRepository.findById(shift.getEmployeeId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Employee not found with id: " + shift.getEmployeeId()));
        List<Shift> existingShifts = shiftRepository.findByEmployeeIdAndDate(shift.getEmployeeId(), shift.getDate());
        for (Shift existing : existingShifts) {
            if (isTimeOverlapping(existing.getStartTime(), existing.getEndTime(), shift.getStartTime(),
                    shift.getEndTime())) {
                throw new IllegalArgumentException("Schedule Conflict: Employee already has an overlapping shift on "
                        + shift.getDate() + " (" + existing.getStartTime() + " - " + existing.getEndTime() + ")");
            }
        }

        shift.setEmployeeName(employee.getName());

        if (shift.getStatus() == null || shift.getStatus().trim().isEmpty()) {
            shift.setStatus("ASSIGNED");
        }

        return shiftRepository.save(shift);
    }

    public Shift updateShift(Long id, Shift shiftDetails) {
        Shift existing = getShiftById(id);
        if (shiftDetails.getDate() != null)
            existing.setDate(shiftDetails.getDate());
        if (shiftDetails.getShiftType() != null)
            existing.setShiftType(shiftDetails.getShiftType());
        if (shiftDetails.getStartTime() != null)
            existing.setStartTime(shiftDetails.getStartTime());
        if (shiftDetails.getEndTime() != null)
            existing.setEndTime(shiftDetails.getEndTime());
        if (shiftDetails.getStatus() != null)
            existing.setStatus(shiftDetails.getStatus());
        if (shiftDetails.getNotes() != null)
            existing.setNotes(shiftDetails.getNotes());
        if (shiftDetails.getEmployeeId() != null) {
            existing.setEmployeeId(shiftDetails.getEmployeeId());
        }
        validateShift(existing);
        Employee employee = employeeRepository.findById(existing.getEmployeeId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Employee not found with id: " + existing.getEmployeeId()));
        existing.setEmployeeName(employee.getName());
        for (Shift other : shiftRepository.findByEmployeeIdAndDate(existing.getEmployeeId(), existing.getDate())) {
            if (!other.getId().equals(existing.getId())
                    && isTimeOverlapping(other.getStartTime(), other.getEndTime(), existing.getStartTime(),
                            existing.getEndTime())) {
                throw new IllegalArgumentException(
                        "Schedule conflict: employee already has an overlapping shift on " + existing.getDate());
            }
        }
        return shiftRepository.save(existing);
    }

    public void deleteShift(Long id) {
        Shift existing = getShiftById(id);
        shiftRepository.delete(existing);
    }

    private boolean isTimeOverlapping(String startA, String endA, String startB, String endB) {
        if (startA == null || endA == null || startB == null || endB == null)
            return false;
        return (startA.compareTo(endB) < 0 && endA.compareTo(startB) > 0);
    }

    private void validateShift(Shift shift) {
        if (shift.getEmployeeId() == null || shift.getDate() == null || shift.getDate().isBlank()
                || shift.getStartTime() == null || shift.getEndTime() == null) {
            throw new IllegalArgumentException("Employee, date, start time, and end time are required");
        }
        try {
            LocalDate.parse(shift.getDate());
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("Shift date must use YYYY-MM-DD format");
        }
        try {
            if (!LocalTime.parse(shift.getStartTime()).isBefore(LocalTime.parse(shift.getEndTime()))) {
                throw new IllegalArgumentException("Shift end time must be after its start time");
            }
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("Shift times must use HH:mm format");
        }
    }
}