package com.clockwork.scheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class MpesaStatementService {

    private static final Logger log = LoggerFactory.getLogger(MpesaStatementService.class);

    public record MpesaTransaction(
            String receiptNo,
            String phone,
            double amount,
            String accountReference,
            Instant transactionTime,
            String status
    ) {}

    public record InternalLedgerEntry(
            String referenceId,
            String customerPhone,
            double expectedAmount,
            String ledgerCode
    ) {}

    public record ReconciliationReport(
            String paybillNumber,
            String accountReference,
            int totalMpesaRecords,
            int totalInternalRecords,
            int matchedCount,
            int mismatchCount,
            double totalSettledAmountKes,
            double discrepancyTotalKes,
            List<String> mismatchDetails,
            String emailStatus
    ) {}

    public ReconciliationReport performReconciliation(String paybill, String accountRef, double threshold, String notifyEmail) {
        log.info("Starting M-Pesa reconciliation for Paybill: {}, Account: {}, Discrepancy Threshold: KES {}",
                paybill, accountRef, threshold);

        List<MpesaTransaction> mpesaTransactions = generateMockMpesaFeed(paybill);
        List<InternalLedgerEntry> ledgerEntries = generateMockInternalLedger();

        Map<String, InternalLedgerEntry> ledgerByRef = new HashMap<>();
        for (InternalLedgerEntry entry : ledgerEntries) {
            ledgerByRef.put(entry.referenceId(), entry);
        }

        int matched = 0;
        int mismatches = 0;
        double totalSettled = 0.0;
        double totalDiscrepancies = 0.0;
        List<String> details = new ArrayList<>();

        for (MpesaTransaction tx : mpesaTransactions) {
            totalSettled += tx.amount();
            InternalLedgerEntry ledger = ledgerByRef.get(tx.receiptNo());

            if (ledger == null) {
                mismatches++;
                totalDiscrepancies += tx.amount();
                details.add(String.format("UNMATCHED_MPESA: Receipt %s (KES %.2f from %s) not found in internal ledger",
                        tx.receiptNo(), tx.amount(), tx.phone()));
            } else {
                double diff = Math.abs(tx.amount() - ledger.expectedAmount());
                if (diff > threshold) {
                    mismatches++;
                    totalDiscrepancies += diff;
                    details.add(String.format("AMOUNT_MISMATCH: Receipt %s expected KES %.2f but M-Pesa recorded KES %.2f (Diff: KES %.2f)",
                            tx.receiptNo(), ledger.expectedAmount(), tx.amount(), diff));
                } else {
                    matched++;
                }
            }
        }

        String emailMsg = String.format("Reconciliation email dispatched to %s. Settled: KES %,.2f, Mismatches: %d (Variance: KES %,.2f)",
                notifyEmail, totalSettled, mismatches, totalDiscrepancies);
        log.info(emailMsg);

        return new ReconciliationReport(
                paybill,
                accountRef,
                mpesaTransactions.size(),
                ledgerEntries.size(),
                matched,
                mismatches,
                totalSettled,
                totalDiscrepancies,
                details,
                emailMsg
        );
    }

    private List<MpesaTransaction> generateMockMpesaFeed(String paybill) {
        List<MpesaTransaction> list = new ArrayList<>();
        list.add(new MpesaTransaction("QGH789KL01", "+254712345001", 15400.0, "KENYA-SETTLEMENT", Instant.now(), "Completed"));
        list.add(new MpesaTransaction("QGH789KL02", "+254722345002", 8750.0, "KENYA-SETTLEMENT", Instant.now(), "Completed"));
        list.add(new MpesaTransaction("QGH789KL03", "+254733345003", 24000.0, "KENYA-SETTLEMENT", Instant.now(), "Completed"));
        list.add(new MpesaTransaction("QGH789KL04", "+254701345004", 4500.0, "KENYA-SETTLEMENT", Instant.now(), "Completed"));
        list.add(new MpesaTransaction("QGH789KL99", "+254799345099", 125000.0, "KENYA-SETTLEMENT", Instant.now(), "Completed")); // intentionally unmatched
        return list;
    }

    private List<InternalLedgerEntry> generateMockInternalLedger() {
        List<InternalLedgerEntry> list = new ArrayList<>();
        list.add(new InternalLedgerEntry("QGH789KL01", "+254712345001", 15400.0, "LEDGER-ACC-01"));
        list.add(new InternalLedgerEntry("QGH789KL02", "+254722345002", 8750.0, "LEDGER-ACC-02"));
        list.add(new InternalLedgerEntry("QGH789KL03", "+254733345003", 24000.0, "LEDGER-ACC-03"));
        list.add(new InternalLedgerEntry("QGH789KL04", "+254701345004", 2500.0, "LEDGER-ACC-04")); // intentionally different amount
        return list;
    }
}
