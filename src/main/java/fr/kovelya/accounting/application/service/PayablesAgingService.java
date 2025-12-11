package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.application.report.SupplierPayableAgingView;

import java.time.LocalDate;
import java.util.List;

public interface PayablesAgingService {

    List<SupplierPayableAgingView> getSupplierAging(LocalDate asOfDate);
}
