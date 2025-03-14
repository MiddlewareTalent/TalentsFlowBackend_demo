



package com.accesshr.emsbackend.EmployeeController;

import com.accesshr.emsbackend.Dto.EmployeeManagerDTO;
import com.accesshr.emsbackend.Dto.LoginDTO;
import com.accesshr.emsbackend.Entity.EmployeeManager;
import com.accesshr.emsbackend.Service.EmployeeManagerService;
import com.accesshr.emsbackend.response.LoginResponse;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/employeeManager")
@CrossOrigin(origins = "https://middlewaretalentsemsdashboard.azurewebsites.net") // Adjust as needed for your frontend
public class EmployeeManagerController {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.container-name}")
    private String containerName;

    private final EmployeeManagerService employeeManagerService;

    public EmployeeManagerController(EmployeeManagerService employeeManagerService) {
        this.employeeManagerService = employeeManagerService;
    }

    // Add Employee method (used by admins to add employees)
    @PostMapping(value = "/add", produces = "application/json")
    public ResponseEntity<?> addEmployee(
            @Valid @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("country") String country,
            @RequestParam("streetAddress") String streetAddress,
            @RequestParam("city") String city,
            @RequestParam("region") String region,
            @RequestParam("postalCode") String postalCode,
            @RequestParam("workingCountry") String workingCountry,
            @RequestParam("nationalInsuranceNumber") String nationalInsuranceNumber,
            @RequestParam("employeeId") String employeeId,
            @RequestParam("corporateEmail") String corporateEmail,
            @RequestParam("jobRole") String jobRole,
            @RequestParam("employmentStatus") String employmentStatus,
            @RequestParam("reportingTo") String reportingTo,
            @RequestParam("role") String role,
            @RequestParam(value="identityCard" ,required = false) MultipartFile identityCard,
            @RequestParam(value = "visa", required = false) MultipartFile visa,
            @RequestParam(value = "otherDocuments", required = false) MultipartFile otherDocuments) {

        try {
            EmployeeManagerDTO employeeManagerDTO = new EmployeeManagerDTO();
            employeeManagerDTO.setFirstName(firstName);
            employeeManagerDTO.setLastName(lastName);
            employeeManagerDTO.setEmail(email);
            employeeManagerDTO.setCountry(country);
            employeeManagerDTO.setStreetAddress(streetAddress);
            employeeManagerDTO.setCity(city);
            employeeManagerDTO.setRegion(region);
            employeeManagerDTO.setPostalCode(postalCode);
            employeeManagerDTO.setWorkingCountry(workingCountry);
            employeeManagerDTO.setNationalInsuranceNumber(nationalInsuranceNumber);
            employeeManagerDTO.setEmployeeId(employeeId);
            employeeManagerDTO.setCorporateEmail(corporateEmail);
            employeeManagerDTO.setJobRole(jobRole);
            employeeManagerDTO.setEmploymentStatus(employmentStatus);
            employeeManagerDTO.setReportingTo(reportingTo);
            employeeManagerDTO.setRole(role);

            // Save files and update DTO fields for certificates
            employeeManagerDTO.setIdentityCard(saveOptionalFile(identityCard, "nationalCard"));
            employeeManagerDTO.setVisa(saveOptionalFile(visa, "visa"));
            employeeManagerDTO.setOtherDocuments(saveOptionalFile(otherDocuments, "otherDocuments"));

            if(employeeManagerService.existsByCorporateEmail(corporateEmail)){
                throw new RuntimeException("Corporate email already exists: "+corporateEmail);
            }
            EmployeeManagerDTO employeeManager = employeeManagerService.addEmployee(employeeManagerDTO);

            return ResponseEntity.ok(employeeManager);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    // Registration endpoint (for Admins)
    @PostMapping(value = "/register", produces = "application/json")
    public ResponseEntity<?> registerAdmin(
            @Valid @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("password") String password ) {

        EmployeeManagerDTO employeeManagerDTO = new EmployeeManagerDTO();
        employeeManagerDTO.setFirstName(firstName);
        employeeManagerDTO.setLastName(lastName);
        employeeManagerDTO.setEmail(email);
        employeeManagerDTO.setCorporateEmail(email); // Set corporate email to the same email for registration
        employeeManagerDTO.setRole("admin"); // Default role for admin
        employeeManagerDTO.setPassword(password); // Set plain text password

        try {
            EmployeeManagerDTO registeredAdmin = employeeManagerService.addAdmin(employeeManagerDTO);
            return ResponseEntity.ok(registeredAdmin);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed: " + e.getMessage());
        }
    }


    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("nationalCard") MultipartFile nationalCard) {
        try {
            String blobName = uploadFIle(nationalCard, "nationalCard");
            return ResponseEntity.ok(blobName);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    public void init(){
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    public String uploadFIle(MultipartFile file, String caption) throws IOException{
        String blobFilename=file.getOriginalFilename();

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        BlobClient blobClient=blobServiceClient
                .getBlobContainerClient(containerName)
                .getBlobClient(blobFilename);

        blobClient.upload(file.getInputStream(), file.getSize(), true);
        String fileUrl=blobClient.getBlobUrl();

        return fileUrl;
    }

    private String saveFile(MultipartFile file, String fileType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The file cannot be null or empty.");
        }

        // Create a BlobContainerClient
        BlobContainerClient blobContainerClient = new BlobClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName)
                .buildClient().getContainerClient();

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Original filename cannot be null.");
        }

        String blobName = fileType + "-" + originalFilename;

        // Log the blob name before uploading
        System.out.println("Uploading to blob name: " + blobName);

        // Upload the file to Blob Storage
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        blobClient.upload(file.getInputStream(), file.getSize(), true);

        return blobName; // Return the blob name or URL if needed
    }


    // Save optional file to Azure Blob Storage
    private String saveOptionalFile(MultipartFile file, String fileType) throws IOException {
        if (file != null && !file.isEmpty()) {
            return uploadFIle(file, fileType);
        }
        return null;
    }

    // New login endpoint
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginDTO loginDTO) {
        LoginResponse response = employeeManagerService.loginEmployee(loginDTO);
        return new ResponseEntity<>(response, response.getStatus() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED);
    }



    // Fetch all employees
    @GetMapping(value = "/employees", produces = "application/json")
    public ResponseEntity<?> getAllEmployees() {
        try {
            List<EmployeeManagerDTO> employees = employeeManagerService.getAllEmployees();
            return ResponseEntity.ok(employees);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch employees: " + e.getMessage());
        }
    }

//    // Fetch a specific employee by ID
//    @GetMapping(value = "/employees/{employeeId}", produces = "application/json")
//    public ResponseEntity<?> getEmployeesById(@PathVariable("employeeId") String  employeeId) {
//        try {
//            EmployeeManagerDTO employee = employeeManagerService.getEmployeeDataById(employeeId);
//            if (employee != null) {
//                return ResponseEntity.ok(employee);
//            } else {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found");
//            }
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch employee: " + e.getMessage());
//        }
//    }

    @GetMapping(value = "/employees/{id}", produces = "application/json")
    public ResponseEntity<?> getEmployeeById(@PathVariable("id") int id){
       try {
           EmployeeManagerDTO employeeId = employeeManagerService.getById(id);
           if (employeeId != null) {
               return ResponseEntity.ok(employeeId);
           }else {
               return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found");
           }
       }catch (Exception e) {
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch employee: " + e.getMessage());
       }
    }

    // Delete an employee by ID
//    @DeleteMapping("/employees/{id}")
//    public ResponseEntity<String> deleteEmployee(@PathVariable("employeeId") int employeeId) {
//        try {
//            boolean isDeleted = employeeManagerService.deleteEmployeeById(employeeId);
//            if (isDeleted) {
//                return ResponseEntity.ok("Employee deleted successfully");
//            } else {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found");
//            }
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete employee: " + e.getMessage());
//        }
//    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateEmployeeData(@PathVariable("id") int id, EmployeeManagerDTO employeeManagerDTO){
        EmployeeManagerDTO updateEmp = employeeManagerService.updateEmployee(id, employeeManagerDTO);
        try {
            return ResponseEntity.ok(updateEmp);
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update employee: " + e.getMessage());
        }
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable("id") int id) {
        try {
            boolean isDeleted = employeeManagerService.deleteById(id);
            if (isDeleted) {
                return ResponseEntity.ok("Employee deleted successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found");
            }
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete employee: " + e.getMessage());
        }
    }


    // New endpoint to get file size
    @GetMapping("/fileSize")
    public ResponseEntity<Map<String, Long>> getFileSize(@RequestParam String fileName) {
        try {
            BlobContainerClient blobContainerClient = new BlobClientBuilder()
                    .connectionString(connectionString)
                    .containerName(containerName)
                    .buildClient().getContainerClient();

            // Get the blob properties
            BlobProperties properties = blobContainerClient.getBlobClient(fileName).getProperties();

            // Prepare response with file size
            Map<String, Long> response = new HashMap<>();
            response.put("size", properties.getBlobSize()); // Size in bytes
            return ResponseEntity.ok(response);
        } catch (BlobStorageException e) {
            // Handle not found error
            if (e.getStatusCode() == 404) {
                Map<String, Long> response = new HashMap<>();
                response.put("size", 0L); // File not found, size is 0
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            // Handle other errors
            Map<String, Long> response = new HashMap<>();
            response.put("size", 0L); // Error occurred, size is 0
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            // Handle any other exceptions
            Map<String, Long> response = new HashMap<>();
            response.put("size", 0L); // Error occurred, size is 0
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping(value="/exists/{employeeId}" , produces = "application/json")
    public Boolean checkEmployeeExists(@PathVariable String employeeId) {
        boolean exists = employeeManagerService.isEmployeeIdPresent(employeeId);

        if (exists) {
            return true;
        } else {
            return false;
        }
    }
    // Method to find the origin employee by their empId
    @GetMapping(value = "/origin/{employeeId}", produces = "application/json")
    public ResponseEntity<List<EmployeeManager>> getOriginEmployee(@PathVariable String employeeId) throws Exception {
        List<EmployeeManager> reportingChain = employeeManagerService.findOrigin(employeeId);

        if (reportingChain.isEmpty()) {
            return ResponseEntity.notFound().build();  // Return 404 if no employee or chain found
        }

        return ResponseEntity.ok(reportingChain);  // Return the list of employees in the reporting chain
    }
    
    
    @GetMapping(value= "/reporting-to/{employeeId}/{workingCountry}" , produces = "application/json")
    public ResponseEntity<List<EmployeeManager>> getEmployeesReportingTo(@PathVariable String employeeId, @PathVariable String workingCountry) throws Exception {
        List<EmployeeManager> reportingEmployees = employeeManagerService.reportingToList(employeeId, workingCountry);

        if (reportingEmployees.isEmpty()) {
            // Throw an exception if no employees are found
            throw new Exception("No employees found reporting to ID: " + employeeId);
        }

        return ResponseEntity.ok(reportingEmployees); // Return the list with 200 OK
    }
    
    @GetMapping(value = "/getEmployee/{employeeId}", produces = "application/json")
    public EmployeeManager getEmployeeById(@PathVariable String employeeId){
    	EmployeeManager employeeManager = employeeManagerService.getEmployeeById(employeeId);
    	return employeeManager;
    }

    @GetMapping(value = "/alsoWorkingWith/{employeeId}/{workingCountry}",produces = "application/json")
    public List<EmployeeManager> getAlsoWorkingWith(@PathVariable String employeeId, @PathVariable String workingCountry) throws Exception {
        List<EmployeeManager> emp=employeeManagerService.alsoWorkingWith(employeeId, workingCountry);
        return emp;
    }

    @PostMapping("/reset-password/{employeeId}/{newPassword}")
    public ResponseEntity<String> changePassword(@PathVariable String employeeId, @PathVariable String newPassword) {
        try {
            // Call the service method to change the password
            employeeManagerService.changePassword(employeeId, newPassword);
            return ResponseEntity.ok("Password changed successfully");
        } catch (IllegalArgumentException e) {
            // Return a bad request response if an error occurs
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody LoginDTO loginDTO) {
        try {
            boolean password = employeeManagerService.changePassword(loginDTO.getEmail(), loginDTO.getOldPassword(), loginDTO.getNewPassword());
            return ResponseEntity.ok(password);
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to change password: " + e.getMessage());
        }
    }

    @GetMapping(value ="/getEmployeesByWorkingCountry/{workingCountry}",produces = "application/json")
    public List <EmployeeManager> getEmployeesByWorkingCountry(@PathVariable String workingCountry){
        return employeeManagerService.getEmployeesByWorkingCountry(workingCountry);
    }

}


