package com.accesshr.emsbackend.Repo;

import com.accesshr.emsbackend.Entity.EmployeeManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeManagerRepository extends JpaRepository<EmployeeManager, Integer> {
    
    // Custom method to find Employee by their corporate email
    EmployeeManager findByCorporateEmail(String corporateEmail);

    // Find by Employee ID
    EmployeeManager findByEmployeeId(String empId);

    // Check if an employee exists by their Employee ID
    boolean existsByEmployeeId(String employeeId);

    // Find Employees by their Reporting Manager
    List<EmployeeManager> findByReportingTo(String empId);

    // Find Employees by their Working Country
    List<EmployeeManager> findByWorkingCountry(String workingCountry);

    boolean existsByCorporateEmail(String corporateEmail);

    // Custom query to find Employees by their Reporting Manager and Working Country
    @Query("SELECT e FROM EmployeeManager e WHERE e.reportingTo = :employeeId AND e.workingCountry = :workingCountry")
    List<EmployeeManager> findReportingTOByWorkingCountry(@Param("employeeId") String employeeId, 
                                                         @Param("workingCountry") String workingCountry);
}
