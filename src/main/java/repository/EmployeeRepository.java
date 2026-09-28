package com.shiftplanner.repository;

import com.shiftplanner.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByStatus(String status);

    List<Employee> findByEmailIgnoreCase(String email);
}
