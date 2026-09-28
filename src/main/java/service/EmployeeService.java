package com.shiftplanner.service;

import com.shiftplanner.entity.Employee;
import com.shiftplanner.exception.ResourceNotFoundException;
import com.shiftplanner.repository.EmployeeRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    public Employee createEmployee(@NonNull Employee employee) {
        if (!isGmailAddress(employee.getEmail())) {
            throw new IllegalArgumentException("Email address must end with @gmail.com");
        }
        if (!employeeRepository.findByEmailIgnoreCase(employee.getEmail()).isEmpty()) {
            throw new IllegalArgumentException("An employee with this email address already exists");
        }
        if (employee.getAvatar() == null || employee.getAvatar().trim().isEmpty()) {
            if (employee.getName() != null && !employee.getName().trim().isEmpty()) {
                String[] parts = employee.getName().trim().split("\\s+");
                StringBuilder initials = new StringBuilder();
                for (String p : parts) {
                    if (!p.isEmpty())
                        initials.append(p.charAt(0));
                }
                employee.setAvatar(initials.toString().toUpperCase());
            } else {
                employee.setAvatar("EM");
            }
        }
        if (employee.getStatus() == null || employee.getStatus().trim().isEmpty()) {
            employee.setStatus("Active");
        }
        return employeeRepository.save(employee);
    }

    public Employee updateEmployee(Long id, Employee employeeDetails) {
        Employee existing = getEmployeeById(id);
        if (employeeDetails.getEmail() != null
                && !employeeDetails.getEmail().equalsIgnoreCase(existing.getEmail())) {
            if (!isGmailAddress(employeeDetails.getEmail())) {
                throw new IllegalArgumentException("Email address must end with @gmail.com");
            }
            if (employeeRepository.findByEmailIgnoreCase(employeeDetails.getEmail())
                    .stream().anyMatch(employee -> !employee.getId().equals(id))) {
                throw new IllegalArgumentException("An employee with this email address already exists");
            }
        }
        if (employeeDetails.getName() != null)
            existing.setName(employeeDetails.getName());
        if (employeeDetails.getEmail() != null)
            existing.setEmail(employeeDetails.getEmail());
        if (employeeDetails.getPhone() != null)
            existing.setPhone(employeeDetails.getPhone());
        if (employeeDetails.getRole() != null)
            existing.setRole(employeeDetails.getRole());
        if (employeeDetails.getDepartment() != null)
            existing.setDepartment(employeeDetails.getDepartment());
        if (employeeDetails.getStatus() != null)
            existing.setStatus(employeeDetails.getStatus());
        if (employeeDetails.getEmploymentType() != null)
            existing.setEmploymentType(employeeDetails.getEmploymentType());
        if (employeeDetails.getMaxHours() != null)
            existing.setMaxHours(employeeDetails.getMaxHours());
        if (employeeDetails.getStartDate() != null)
            existing.setStartDate(employeeDetails.getStartDate());
        if (employeeDetails.getAvatar() != null)
            existing.setAvatar(employeeDetails.getAvatar());
        if (employeeDetails.getDob() != null)
            existing.setDob(employeeDetails.getDob());
        if (employeeDetails.getGender() != null)
            existing.setGender(employeeDetails.getGender());
        if (employeeDetails.getAddress() != null)
            existing.setAddress(employeeDetails.getAddress());
        if (employeeDetails.getNotes() != null)
            existing.setNotes(employeeDetails.getNotes());
        return employeeRepository.save(existing);
    }

    public void deleteEmployee(Long id) {
        Employee existing = getEmployeeById(id);
        employeeRepository.delete(existing);
    }

    private boolean isGmailAddress(String email) {
        return email != null && email.matches("(?i)^[^\\s@]+@gmail\\.com$");
    }
}
