package smon.scan;

import smon.extractor.ExtractionResult;
import smon.mail.MailKind;
import smon.model.Shop;

import java.util.function.Function;
import java.util.function.Predicate;

/** Everything a scan needs from its command. */
public final class ScanRequest {

    private final String shopName;
    private final ScanMode mode;
    private final Function<Shop, ExtractionResult> extractor;
    private final Predicate<Shop> applicable;
    private final MailKind mailKind;
    private final String[] mailParams;

    /**
     * @param shopName   one shop (-n), or null for all shops
     * @param extractor  downloads and extracts one shop
     * @param applicable which shops take part (search skips shops without a search URL)
     * @param mailKind   the report's kind (scan or search)
     * @param mailParams parameters of the report's title, e.g. the search text
     */
    public ScanRequest(String shopName, ScanMode mode, Function<Shop, ExtractionResult> extractor,
                       Predicate<Shop> applicable, MailKind mailKind, String... mailParams) {
        this.shopName = shopName;
        this.mode = mode;
        this.extractor = extractor;
        this.applicable = applicable;
        this.mailKind = mailKind;
        this.mailParams = mailParams;
    }

    public String getShopName() {
        return shopName;
    }

    public ScanMode getMode() {
        return mode;
    }

    public Function<Shop, ExtractionResult> getExtractor() {
        return extractor;
    }

    public Predicate<Shop> getApplicable() {
        return applicable;
    }

    public MailKind getMailKind() {
        return mailKind;
    }

    public String[] getMailParams() {
        return mailParams;
    }
}
