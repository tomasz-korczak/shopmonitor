package smon.util;

import java.math.BigDecimal;

/**
 * Prices as {@link BigDecimal} with scale 2, matching the DECIMAL(10,2) column of ITEMS.PRICE.
 * Shop pages can be message boards, so any text can become a "price": a value the column cannot store, or one with
 * more than 2 decimals, is not a price and is rejected here, before it reaches the database.
 */
public final class Price {

    private static final BigDecimal MAX = new BigDecimal("99999999.99");

    private Price() {
    }

    /**
     * @param resolved the output of PriceResolver, e.g. "1299", "1299.5" or "1299.00"
     * @return the price with scale 2, or null when the input is missing, not a number, negative, above
     *         99,999,999.99 or has more than 2 decimals
     */
    public static BigDecimal parse(String resolved) {
        if (resolved == null || resolved.isEmpty()) {
            return null;
        }
        BigDecimal value;
        try {
            value = new BigDecimal(resolved);
        } catch (NumberFormatException e) {
            return null;
        }
        if (value.signum() < 0 || value.compareTo(MAX) > 0 || value.stripTrailingZeros().scale() > 2) {
            return null;
        }
        return value.setScale(2);
    }

    /** @return the price as "1299,00" (two decimals, comma as the decimal separator), or "" for null */
    public static String format(BigDecimal price) {
        if (price == null) {
            return "";
        }
        return price.setScale(2).toPlainString().replace('.', ',');
    }
}
