package fr.kovelya.accounting.application;

import java.time.LocalDate;
import java.util.List;

public interface ReceivablesAgingService {

    List<CustomerReceivableAgingView> getCustomerAging(LocalDate asOfDate);
}
