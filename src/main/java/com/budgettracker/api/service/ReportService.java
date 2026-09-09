package com.budgettracker.api.service;

import com.budgettracker.api.entity.*;
import com.budgettracker.api.exception.ApiException;
import com.budgettracker.api.repository.*;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Génère les rapports mensuels/annuels (CSV + PDF) — fonctionnalité "traçabilité
 * financière" pour montrer à une banque, Visa, ou un associé.
 */
@Service
public class ReportService {

    private static final String[] MOIS_FR = {
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    private final SettingsRepository settingsRepository;
    private final MonthSettingsRepository monthSettingsRepository;
    private final ExpenseRepository expenseRepository;
    private final CarryOverRepository carryOverRepository;

    public ReportService(SettingsRepository settingsRepository,
                          MonthSettingsRepository monthSettingsRepository,
                          ExpenseRepository expenseRepository,
                          CarryOverRepository carryOverRepository) {
        this.settingsRepository = settingsRepository;
        this.monthSettingsRepository = monthSettingsRepository;
        this.expenseRepository = expenseRepository;
        this.carryOverRepository = carryOverRepository;
    }

    // ── Résolution des données d'un mois ────────────────────────────────────────

    private record MonthReport(
            String monthKey, String label, int salary, int savings, int carryOver,
            List<Expense> expenses, int totalExpenses, Map<String, Integer> byCategory, int disponible
    ) {
    }

    private String labelFor(String monthKey) {
        String[] parts = monthKey.split("-");
        int monthIdx = Integer.parseInt(parts[1]) - 1;
        return MOIS_FR[monthIdx] + " " + parts[0];
    }

    /**
     * "Disponible" = revenu + report reçu - dépenses (l'épargne visée est affichée à
     * titre indicatif, pas déduite ici). Si ta logique budgétaire côté front calcule
     * ça différemment, ajuste cette formule pour rester cohérent entre les deux.
     */
    private MonthReport buildMonthReport(String userId, String monthKey) {
        Settings settings = settingsRepository.findById(userId).orElse(null);
        int defaultSalary = settings != null ? settings.getSalary() : 150000;
        int defaultSavings = settings != null ? settings.getSavings() : 30000;

        MonthSettingsId msId = new MonthSettingsId(userId, monthKey);
        MonthSettings override = monthSettingsRepository.findById(msId).orElse(null);
        int salary = (override != null && override.getSalary() != null) ? override.getSalary() : defaultSalary;
        int savings = (override != null && override.getSavings() != null) ? override.getSavings() : defaultSavings;

        CarryOverId coId = new CarryOverId(userId, monthKey);
        int carryOver = carryOverRepository.findById(coId).map(CarryOver::getAmount).orElse(0);

        List<Expense> expenses = expenseRepository.findByUserIdAndMonthKeyOrderByDateAsc(userId, monthKey);
        int totalExpenses = expenses.stream().mapToInt(Expense::getAmount).sum();

        Map<String, Integer> byCategory = new LinkedHashMap<>();
        for (Expense e : expenses) {
            byCategory.merge(e.getCategory(), e.getAmount(), Integer::sum);
        }

        int disponible = salary + carryOver - totalExpenses;

        return new MonthReport(monthKey, labelFor(monthKey), salary, savings, carryOver,
                expenses, totalExpenses, byCategory, disponible);
    }

    private List<String> monthKeysOfYear(String year) {
        List<String> keys = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            keys.add(year + "-" + String.format("%02d", m));
        }
        return keys;
    }

    // ── CSV ──────────────────────────────────────────────────────────────────────

    public String generateMonthlyCsv(String userId, String monthKey) {
        return generateCsv(userId, List.of(monthKey), "Rapport mensuel - " + labelFor(monthKey));
    }

    public String generateAnnualCsv(String userId, String year) {
        return generateCsv(userId, monthKeysOfYear(year), "Rapport annuel - " + year);
    }

    /**
     * Rapport combiné pour une liste de mois arbitraire (1 à N mois, pas forcément
     * consécutifs) — utilisé quand l'utilisateur choisit "1, 2 ou 3 mois" au lieu
     * d'un mois unique ou d'une année complète.
     */
    public String generateCsv(String userId, List<String> monthKeys, String title) {
        List<String> sorted = monthKeys.stream().sorted().toList();
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n\n");

        if (sorted.size() > 1) {
            sb.append("Mois;Revenu;Report;Depenses;Disponible\n");
            int totalSalary = 0, totalExpenses = 0;
            List<MonthReport> reports = new ArrayList<>();
            for (String mk : sorted) {
                MonthReport r = buildMonthReport(userId, mk);
                reports.add(r);
                sb.append(r.label()).append(";")
                        .append(r.salary()).append(";")
                        .append(r.carryOver()).append(";")
                        .append(r.totalExpenses()).append(";")
                        .append(r.disponible()).append("\n");
                totalSalary += r.salary();
                totalExpenses += r.totalExpenses();
            }
            sb.append("TOTAL;").append(totalSalary).append(";;").append(totalExpenses).append(";\n\n");

            sb.append("Detail des depenses\n");
            sb.append("Mois;Date;Categorie;Description;Montant (F CFA)\n");
            for (MonthReport r : reports) {
                for (Expense e : r.expenses()) {
                    sb.append(r.label()).append(";")
                            .append(e.getDate().toString(), 0, 10).append(";")
                            .append(csvEscape(e.getCategory())).append(";")
                            .append(csvEscape(e.getDescription())).append(";")
                            .append(e.getAmount()).append("\n");
                }
            }
        } else {
            MonthReport r = buildMonthReport(userId, sorted.get(0));
            sb.append("Revenu;").append(r.salary()).append("\n");
            sb.append("Report du mois precedent;").append(r.carryOver()).append("\n");
            sb.append("Epargne visee;").append(r.savings()).append("\n");
            sb.append("Total depenses;").append(r.totalExpenses()).append("\n");
            sb.append("Disponible (revenu + report - depenses);").append(r.disponible()).append("\n\n");
            sb.append("Date;Categorie;Description;Montant (F CFA)\n");
            for (Expense e : r.expenses()) {
                sb.append(e.getDate().toString(), 0, 10).append(";")
                        .append(csvEscape(e.getCategory())).append(";")
                        .append(csvEscape(e.getDescription())).append(";")
                        .append(e.getAmount()).append("\n");
            }
        }
        return sb.toString();
    }

    private String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // ── PDF ──────────────────────────────────────────────────────────────────────

    public byte[] generateMonthlyPdf(String userId, String monthKey) {
        MonthReport r = buildMonthReport(userId, monthKey);
        return renderPdf("Rapport mensuel — " + r.label(), List.of(r));
    }

    public byte[] generateAnnualPdf(String userId, String year) {
        List<MonthReport> reports = new ArrayList<>();
        for (String mk : monthKeysOfYear(year)) {
            reports.add(buildMonthReport(userId, mk));
        }
        return renderPdf("Rapport annuel — " + year, reports);
    }

    /** Équivalent PDF de generateCsv(userId, monthKeys, title) — 1 à N mois arbitraires. */
    public byte[] generatePdf(String userId, List<String> monthKeys, String title) {
        List<MonthReport> reports = monthKeys.stream()
                .sorted()
                .map(mk -> buildMonthReport(userId, mk))
                .toList();
        return renderPdf(title, reports);
    }

    private byte[] renderPdf(String title, List<MonthReport> reports) {
        try {
            Document document = new Document(PageSize.A4, 40, 40, 50, 50);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            document.add(new Paragraph(title, titleFont));
            document.add(new Paragraph(" "));

            int totalExpensesAll = 0, totalSalaryAll = 0;

            for (MonthReport r : reports) {
                document.add(new Paragraph(r.label(), sectionFont));

                PdfPTable summary = new PdfPTable(2);
                summary.setWidthPercentage(60);
                summary.setSpacingBefore(6);
                summary.setSpacingAfter(10);
                addSummaryRow(summary, "Revenu", fmt(r.salary()), normalFont, boldFont);
                addSummaryRow(summary, "Report du mois précédent", fmt(r.carryOver()), normalFont, boldFont);
                addSummaryRow(summary, "Épargne visée", fmt(r.savings()), normalFont, boldFont);
                addSummaryRow(summary, "Total dépenses", fmt(r.totalExpenses()), normalFont, boldFont);
                addSummaryRow(summary, "Disponible", fmt(r.disponible()), normalFont, boldFont);
                document.add(summary);

                if (!r.expenses().isEmpty()) {
                    PdfPTable table = new PdfPTable(4);
                    table.setWidthPercentage(100);
                    table.setWidths(new float[]{15, 20, 45, 20});
                    addHeaderCell(table, "Date", boldFont);
                    addHeaderCell(table, "Catégorie", boldFont);
                    addHeaderCell(table, "Description", boldFont);
                    addHeaderCell(table, "Montant", boldFont);

                    for (Expense e : r.expenses()) {
                        table.addCell(new Phrase(e.getDate().toString().substring(0, 10), normalFont));
                        table.addCell(new Phrase(e.getCategory(), normalFont));
                        table.addCell(new Phrase(e.getDescription(), normalFont));
                        table.addCell(new Phrase(fmt(e.getAmount()) + " F", normalFont));
                    }
                    document.add(table);
                } else {
                    document.add(new Paragraph("Aucune dépense enregistrée ce mois.", normalFont));
                }

                document.add(new Paragraph(" "));
                totalExpensesAll += r.totalExpenses();
                totalSalaryAll += r.salary();
            }

            if (reports.size() > 1) {
                document.add(new Paragraph("Récapitulatif annuel", sectionFont));
                PdfPTable totals = new PdfPTable(2);
                totals.setWidthPercentage(60);
                totals.setSpacingBefore(6);
                addSummaryRow(totals, "Total revenus", fmt(totalSalaryAll), normalFont, boldFont);
                addSummaryRow(totals, "Total dépenses", fmt(totalExpensesAll), normalFont, boldFont);
                document.add(totals);
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur génération PDF");
        }
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value + " F CFA", valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(4);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(230, 230, 230));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private String fmt(int n) {
        return String.format("%,d", n).replace(",", " ");
    }
}
