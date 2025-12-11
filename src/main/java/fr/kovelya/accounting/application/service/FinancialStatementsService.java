package fr.kovelya.accounting.application.service;

import fr.kovelya.accounting.application.report.BalanceSheetView;
import fr.kovelya.accounting.application.report.IncomeStatementView;
import fr.kovelya.accounting.domain.period.AccountingPeriod;

public interface FinancialStatementsService {

    IncomeStatementView getIncomeStatement(AccountingPeriod period);

    BalanceSheetView getBalanceSheet(AccountingPeriod period);
}
