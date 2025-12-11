package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.application.report.CustomerReceivableAgingView;

import java.time.LocalDate;
import java.util.List;

public interface ReceivablesAgingService {

    List<CustomerReceivableAgingView> getCustomerAging(LocalDate asOfDate);
}
