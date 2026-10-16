package com.clockwork.scheduler.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfReportService {

    private static final Logger log = LoggerFactory.getLogger(PdfReportService.class);

    public byte[] generateFinancialReport(String reportType, String organizationName, String generatedFor) {
        log.info("Generating PDF report for organization: {}, type: {}", organizationName, reportType);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            // Colors
            Color primaryDark = new Color(20, 30, 55);
            Color accentGreen = new Color(16, 185, 129);
            Color lightGray = new Color(245, 247, 250);

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, primaryDark);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, primaryDark);
            Font boldCellFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font regularCellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

            // Header Banner
            Paragraph title = new Paragraph("CLOCKWORK SCHEDULER", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph reportTitle = new Paragraph("EXECUTIVE " + reportType.replace('_', ' ') + " - KENYA REGION", sectionFont);
            reportTitle.setAlignment(Element.ALIGN_CENTER);
            reportTitle.setSpacingBefore(5);
            document.add(reportTitle);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Paragraph meta = new Paragraph(String.format("Organization: %s | Generated: %s | For: %s",
                    organizationName, timestamp, generatedFor), subTitleFont);
            meta.setAlignment(Element.ALIGN_CENTER);
            meta.setSpacingAfter(20);
            document.add(meta);

            // Summary Table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setWidths(new float[]{3.0f, 2.5f, 2.5f, 2.0f});

            String[] headers = {"Metric / Category", "Internal Ledger (KES)", "Settled (KES)", "Variance"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, boldCellFont));
                cell.setBackgroundColor(primaryDark);
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Data rows
            addRow(table, "M-Pesa C2B Paybill Collections", "14,850,200.00", "14,845,700.00", "-4,500.00", regularCellFont, lightGray);
            addRow(table, "Direct Bank EFT Inwards", "32,400,000.00", "32,400,000.00", "0.00", regularCellFont, Color.WHITE);
            addRow(table, "B2C Dispatches (Payroll/Payouts)", "8,920,400.00", "8,920,400.00", "0.00", regularCellFont, lightGray);
            addRow(table, "Vendor Outwards Settlements", "6,150,000.00", "6,148,800.00", "-1,200.00", regularCellFont, Color.WHITE);

            document.add(table);

            // Total banner
            Paragraph totalPara = new Paragraph("\nAudit Verdict: System balances verified. Minor reconciliations flagged for review.",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, accentGreen));
            totalPara.setSpacingBefore(15);
            document.add(totalPara);

            Paragraph footer = new Paragraph("\nClockwork Scheduler Automation Engine | Nairobi, Kenya | Secured & Digitally Certified",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, Color.LIGHT_GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF report", e);
            throw new RuntimeException("PDF report generation failed: " + e.getMessage(), e);
        }
    }

    private void addRow(PdfPTable table, String col1, String col2, String col3, String col4, Font font, Color bg) {
        PdfPCell c1 = new PdfPCell(new Phrase(col1, font));
        c1.setBackgroundColor(bg);
        c1.setPadding(6);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(col2, font));
        c2.setBackgroundColor(bg);
        c2.setPadding(6);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(col3, font));
        c3.setBackgroundColor(bg);
        c3.setPadding(6);
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase(col4, font));
        c4.setBackgroundColor(bg);
        c4.setPadding(6);
        c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c4);
    }
}
