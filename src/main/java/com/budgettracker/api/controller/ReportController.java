package com.budgettracker.api.controller;

import com.budgettracker.api.exception.ApiException;
import com.budgettracker.api.security.AuthenticatedUser;
import com.budgettracker.api.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Exports "traçabilité financière" — CSV et PDF.
 * Passer un seul de ces trois paramètres :
 *   ?month=2026-08              → un mois précis
 *   ?months=2026-06,2026-08     → 1 à N mois arbitraires (pas forcément consécutifs)
 *   ?year=2026                  → année complète (12 mois)
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // GET /api/reports/csv?month=2026-08 | ?months=2026-06,2026-08 | ?year=2026
    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportCsv(@AuthenticationPrincipal AuthenticatedUser user,
                                             @RequestParam(required = false) String month,
                                             @RequestParam(required = false) String months,
                                             @RequestParam(required = false) String year) {
        String filename;
        String csv;
        if (months != null && !months.isBlank()) {
            List<String> keys = parseMonths(months);
            csv = reportService.generateCsv(user.userId(), keys, titleFor(keys));
            filename = "rapport-" + String.join("_", keys) + ".csv";
        } else if (month != null) {
            csv = reportService.generateMonthlyCsv(user.userId(), month);
            filename = "rapport-" + month + ".csv";
        } else if (year != null) {
            csv = reportService.generateAnnualCsv(user.userId(), year);
            filename = "rapport-" + year + ".csv";
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Paramètre 'month', 'months' ou 'year' requis");
        }

        // BOM UTF-8 en préfixe pour qu'Excel affiche correctement les accents à l'ouverture
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        byte[] full = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, full, 0, bom.length);
        System.arraycopy(body, 0, full, bom.length, body.length);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(full);
    }

    // GET /api/reports/pdf?month=2026-08 | ?months=2026-06,2026-08 | ?year=2026
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(@AuthenticationPrincipal AuthenticatedUser user,
                                             @RequestParam(required = false) String month,
                                             @RequestParam(required = false) String months,
                                             @RequestParam(required = false) String year) {
        String filename;
        byte[] pdf;
        if (months != null && !months.isBlank()) {
            List<String> keys = parseMonths(months);
            pdf = reportService.generatePdf(user.userId(), keys, titleFor(keys));
            filename = "rapport-" + String.join("_", keys) + ".pdf";
        } else if (month != null) {
            pdf = reportService.generateMonthlyPdf(user.userId(), month);
            filename = "rapport-" + month + ".pdf";
        } else if (year != null) {
            pdf = reportService.generateAnnualPdf(user.userId(), year);
            filename = "rapport-" + year + ".pdf";
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Paramètre 'month', 'months' ou 'year' requis");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(pdf);
    }

    private List<String> parseMonths(String raw) {
        List<String> keys = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .toList();
        if (keys.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "'months' ne contient aucun mois valide");
        }
        return keys;
    }

    private String titleFor(List<String> keys) {
        if (keys.size() == 1) {
            return "Rapport mensuel - " + keys.get(0);
        }
        return "Rapport - " + keys.get(0) + " à " + keys.get(keys.size() - 1);
    }
}
