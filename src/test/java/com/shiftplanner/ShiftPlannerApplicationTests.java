package com.shiftplanner;

import com.shiftplanner.entity.Employee;
import com.shiftplanner.entity.Shift;
import com.shiftplanner.entity.SwapRequest;
import com.shiftplanner.repository.EmployeeRepository;
import com.shiftplanner.repository.ShiftRepository;
import com.shiftplanner.repository.SwapRequestRepository;
import com.shiftplanner.service.EmployeeService;
import com.shiftplanner.service.SwapRequestService;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ShiftPlannerApplicationTests {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private SwapRequestRepository swapRequestRepository;

    @Autowired
    private SwapRequestService swapRequestService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private Validator validator;

    @BeforeEach
    void clearData() {
        swapRequestRepository.deleteAll();
        shiftRepository.deleteAll();
        employeeRepository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void approvedRequestTransfersShiftOnlyAfterManagerApproval() {
        Employee requester = saveEmployee("Requester", "requester@example.com");
        Employee colleague = saveEmployee("Colleague", "colleague@example.com");
        Shift shift = saveShift(requester.getId());
        SwapRequest request = createRequest(requester, colleague, shift);

        assertEquals("SWAP_PENDING", shiftRepository.findById(shift.getId()).orElseThrow().getStatus());
        SwapRequest accepted = swapRequestService.colleagueRespond(request.getId(), "ACCEPT");
        assertEquals("AWAITING_MANAGER", accepted.getStatus());
        assertEquals(requester.getId(), shiftRepository.findById(shift.getId()).orElseThrow().getEmployeeId());

        SwapRequest approved = swapRequestService.managerDecision(request.getId(), "APPROVE", null);
        assertEquals("APPROVED", approved.getStatus());
        assertEquals(colleague.getId(), shiftRepository.findById(shift.getId()).orElseThrow().getEmployeeId());
        assertThrows(IllegalArgumentException.class,
                () -> swapRequestService.managerDecision(request.getId(), "APPROVE", null));
    }

    @Test
    void rejectedRequestLeavesShiftWithOriginalEmployee() {
        Employee requester = saveEmployee("Requester", "requester@example.com");
        Employee colleague = saveEmployee("Colleague", "colleague@example.com");
        Shift shift = saveShift(requester.getId());
        SwapRequest request = createRequest(requester, colleague, shift);
        swapRequestService.colleagueRespond(request.getId(), "ACCEPT");

        SwapRequest rejected = swapRequestService.managerDecision(request.getId(), "REJECT", "Coverage required");

        assertEquals("REJECTED", rejected.getStatus());
        assertEquals(requester.getId(), shiftRepository.findById(shift.getId()).orElseThrow().getEmployeeId());
        assertEquals("ASSIGNED", shiftRepository.findById(shift.getId()).orElseThrow().getStatus());
    }

    @Test
    void duplicateActiveRequestForShiftIsRejected() {
        Employee requester = saveEmployee("Requester", "requester@example.com");
        Employee colleague = saveEmployee("Colleague", "colleague@example.com");
        Shift shift = saveShift(requester.getId());
        createRequest(requester, colleague, shift);

        assertThrows(IllegalArgumentException.class, () -> createRequest(requester, colleague, shift));
    }

    @Test
    void editingEmployeeWithExistingDuplicateEmailIsAllowedIfEmailIsUnchanged() {
        Employee first = saveEmployee("First", "duplicate@example.com");
        saveEmployee("Second", "duplicate@example.com");
        Employee newEmployee = saveEmployee("New", "new@example.com");

        Employee details = new Employee();
        details.setName("Updated First");
        details.setEmail(first.getEmail());

        assertEquals("Updated First", employeeService.updateEmployee(first.getId(), details).getName());
        details.setEmail("duplicate@example.com");
        assertThrows(IllegalArgumentException.class,
                () -> employeeService.updateEmployee(newEmployee.getId(), details));
    }

    @Test
    void employeeEmailMustUseGmailDomain() {
        Employee employee = new Employee();
        employee.setName("Invalid Domain");
        employee.setEmail("person@gmail.comx");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> employeeService.createEmployee(employee));
        assertTrue(exception.getMessage().contains("@gmail.com"));
    }

    @Test
    void employeeWithGmailAddressCanBeCreated() {
        Employee employee = new Employee();
        employee.setName("Valid Gmail");
        employee.setEmail("valid.person@gmail.com");

        assertEquals("valid.person@gmail.com", employeeService.createEmployee(employee).getEmail());
    }

    @Test
    void employeeNameEmailAndHoursAreRequired() {
        Employee employee = new Employee();
        employee.setMaxHours(null);

        List<String> invalidFields = validator.validate(employee).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();

        assertTrue(invalidFields.containsAll(List.of("name", "email", "maxHours")));
    }

    private Employee saveEmployee(String name, String email) {
        Employee employee = new Employee();
        employee.setName(name);
        employee.setEmail(email);
        return employeeRepository.save(employee);
    }

    private Shift saveShift(Long employeeId) {
        Shift shift = new Shift();
        shift.setEmployeeId(employeeId);
        shift.setDate("2026-10-01");
        shift.setShiftType("DAY");
        shift.setStartTime("09:00");
        shift.setEndTime("17:00");
        shift.setStatus("ASSIGNED");
        return shiftRepository.save(shift);
    }

    private SwapRequest createRequest(Employee requester, Employee colleague, Shift shift) {
        SwapRequest request = new SwapRequest();
        request.setRequesterId(requester.getId());
        request.setColleagueId(colleague.getId());
        request.setShiftId(shift.getId());
        return swapRequestService.createSwapRequest(request);
    }

}
