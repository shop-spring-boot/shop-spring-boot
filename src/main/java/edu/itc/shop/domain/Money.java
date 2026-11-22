package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** An amount with a currency, written as "12.50 EUR". */
public record Money(BigDecimal amount, String currency) {

    private static final Pattern FORMAT = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s+([A-Z]{3})\\s*$");

    public static Money parse(String text) {
        Matcher m = text == null ? null : FORMAT.matcher(text);
        if (m == null || !m.matches()) {
            throw new IllegalArgumentException("Cannot parse Money from '" + text + "'");
        }
        return new Money(new BigDecimal(m.group(1)), m.group(2));
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
