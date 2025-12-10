package fr.kovelya.bootstrap;

import fr.kovelya.domain.model.Money;

import java.math.BigDecimal;
import java.util.Currency;

public class ConsoleApp {
    public static void main(String[] args) {
        Money amount = Money.of(new BigDecimal("100.00"), Currency.getInstance("EUR"));
        System.out.println("Kovelya Extreme Accounting is alive with amount: " + amount);
    }
}
