package smon.extractor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a scraped price text ("1 299,00 zł", "Cena: 1.299 zł", "od 100 zł do 200 zł") into a plain number string
 * ("1299.00", "1299", "100") that {@link Double#parseDouble} accepts.
 * <p>
 * The <b>first</b> number in the text is taken; a later one (a range end, an old price) is ignored.
 * Separators (space, no-break space, narrow no-break space, '.', ',' or '):
 * <ul>
 *   <li>a separator followed by 3 or more digits is a thousands separator, so "1.299" is 1299 and "1.2345" is 12345;</li>
 *   <li>a last separator followed by 1 or 2 digits is the decimal separator, so "2.5" is 2.5 and "1 299,00" is 1299.00.</li>
 * </ul>
 * This rule is the project owner's (shop data comes from several countries). A number whose separators do not fit
 * the rule is not handled specially.
 */
public class PriceResolver {

    /** A number: digit groups joined by single separators. Two numbers separated by one space join into one candidate. */
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[ \\u00a0\\u202f.,']\\d+)*");
    private static final Pattern SEPARATOR = Pattern.compile("[ \\u00a0\\u202f.,']");

    private final String sourcePrice;

    public PriceResolver(String sourcePrice) {
        this.sourcePrice = sourcePrice;
    }

    /** @return the price as "1234" or "1234.5", or null when the text contains no number */
    public String resolve() {
        if (sourcePrice == null) {
            return null;
        }
        Matcher matcher = NUMBER.matcher(sourcePrice);
        if (!matcher.find()) {
            return null;
        }
        String[] groups = SEPARATOR.split(matcher.group());
        String last = groups[groups.length - 1];
        if (groups.length >= 2 && last.length() <= 2) {
            StringBuilder integer = new StringBuilder();
            for (int i = 0; i < groups.length - 1; i++) {
                integer.append(groups[i]);
            }
            return integer + "." + last;
        }
        StringBuilder all = new StringBuilder();
        for (String group : groups) {
            all.append(group);
        }
        return all.toString();
    }
}
