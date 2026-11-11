package smon.items;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The identity of an item: link plus price. Both the stored rows and the scraped items go through this, so the
 * two sides compare equal when they are the same product at the same price (scale 2, null price included).
 */
public final class ItemKey {

    private ItemKey() {
    }

    public static String of(String link, BigDecimal price) {
        String priceText = price == null ? "null" : price.setScale(2, RoundingMode.HALF_UP).toPlainString();
        return link + "|" + priceText;
    }
}
