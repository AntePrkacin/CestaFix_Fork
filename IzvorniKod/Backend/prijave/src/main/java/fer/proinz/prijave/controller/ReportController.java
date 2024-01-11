package fer.proinz.prijave.controller;

import fer.proinz.prijave.dto.CreateReportRequestDto;
import fer.proinz.prijave.model.*;
import fer.proinz.prijave.service.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class ReportController {
    @Autowired
    private final ReportService reportService;
    @Autowired
    private final ProblemService problemService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private PhotoService photoService;

    @Operation(summary = "Get all reports")
    @GetMapping( "/public/report/getAll")
    public List<Report> getAllReports() {
        return reportService.getAllReports();
    }

    @Operation(summary = "Get a report by its id")
    @GetMapping( "/public/report/{reportId}")
    public ResponseEntity<Report> getReportById(@PathVariable("reportId") int reportId) {
        return reportService.getReportById(reportId);
    }

    @Operation(summary = "Create a report")
    @PostMapping("/public/report")
    @Transactional
    public ResponseEntity<?> createReport(@RequestBody CreateReportRequestDto reportRequest, HttpServletRequest httpRequest) {

        Optional<Category> optionalCategory = categoryService.getCategoryById(reportRequest.getCategoryId());
        if (optionalCategory.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Kategorija nije pronadena");
        }
        Category category = optionalCategory.get();

        User user = null;
        String authorizationHeader = httpRequest.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            Integer userId = jwtService.extractUserId(token);
            Optional<User> optionalUser = userService.getUserById(userId);
            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Korisnik tokena nije pronaden.");
            }
            user = optionalUser.get();
        }

        Problem savedProblem = null;
        if (reportRequest.getMergeProblemId() == null) {
            Problem problem = Problem.builder()
                    .longitude(reportRequest.getLongitude())
                    .latitude(reportRequest.getLatitude())
                    .status(reportRequest.getProblemStatus())
                    .category(category)
                    .build();

            // Save the Problem object
            savedProblem = problemService.createProblem(problem);
            if (savedProblem == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Nije moguce stvoriti problem objekt");
            }
        } else {
            Optional<Problem> optionalProblem = problemService.getProblemById(reportRequest.getMergeProblemId());
            if (optionalProblem.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Problem za merge nije pronaden");
            }
            savedProblem = optionalProblem.get();
        }

        // Create new Report
        Report report = Report.builder()
                .title(reportRequest.getTitle())
                .description(reportRequest.getDescription())
                .address(reportRequest.getAddress())
                .base64Photos(reportRequest.getBase64Photos())
                .status(reportRequest.getReportStatus())
                .longitude(reportRequest.getLongitude())
                .latitude(reportRequest.getLatitude())
                .problem(savedProblem)
                .user(user)
                .build();

        //
        if (report.getBase64Photos() != null) {
            List<Photo> photos = new ArrayList<>();
            for (int i = 0; i < report.getBase64Photos().size(); i++) {
                String base64Photo = report.getBase64Photos().get(i);

                // Check if the string is long enough before attempting to substring
                if (base64Photo.length() > 22) {
                    base64Photo = base64Photo.substring(22);
                    report.getBase64Photos().set(i, base64Photo);
                }

                Photo photo = Photo.builder()
                        .photoData(Base64.getDecoder().decode(base64Photo))
                        .report(report)
                        .build();
                photoService.createPhoto(photo);
                photos.add(photo);
            }

            report.setPhotos(photos);
        } else {
            report.setPhotos(null);
        }

        // Save the Report object
        Report savedReport = reportService.createReport(report);
        entityManager.refresh(savedReport);
        return ResponseEntity.ok(savedReport);
    }

    @Operation(summary = "Update a report")
    @PatchMapping("/advanced/report/{reportId}")
    public ResponseEntity<?> updateReport(
            @PathVariable("reportId") int reportId,
            @RequestBody Report updatedReport
        ) {
        return reportService.updateReport(reportId, updatedReport);
    }

    @Operation(summary = "Staff member groups reports")
    @PatchMapping("/advanced/report/group/{problemId}")
    public ResponseEntity<?> groupReports(
            @PathVariable("problemId") int problemId,
            @RequestBody List<Report> reportList
    ) {
        return reportService.groupReports(problemId, reportList);
    }

    @Operation(summary = "Delete a report")
    @DeleteMapping("/advanced/report/{reportId}")
    public ResponseEntity<String> deleteReport(@PathVariable("reportId") int reportId) {
        return reportService.deleteReport(reportId);
    }

}
