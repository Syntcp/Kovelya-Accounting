package fr.kovelya.accounting.application;

import java.math.BigDecimal;

public record InvoiceLineRequest(String description, BigDecimal amount) {

}
