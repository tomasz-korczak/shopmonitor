package smon.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import smon.extractor.SearchItemsExtractor;
import smon.items.service.ItemsService;
import smon.mail.MailKind;
import smon.model.Shop;
import smon.scan.ScanMode;
import smon.scan.ScanOutcome;
import smon.scan.ScanRequest;
import smon.scan.ScanService;

@Slf4j
@Command(name = "search", description = "Search for items using online shop search engines. Read only, found items are not saved.")
public class SearchItemsCommand extends BaseCommand {

    @Required
    @Option(name = {"-s" }, description = "search input string")
    private String searchString;

    @Option(name = {"-n", "--name"}, description = "shop name, when supplied limits scanning to this shop only")
    private String shopName;

    @Option(name = {"-nm", "--no-mail"}, description = "do not send email report")
    private boolean noMail;

    @Autowired
    private ScanService scanService;

    @Autowired
    private ItemsService itemsService;

    @Override
    public void run() {
        ScanMode mode = ScanMode.forSearch(noMail);
        ScanOutcome outcome = scanService.scan(new ScanRequest(shopName, mode,
                shop -> new SearchItemsExtractor(shop, searchString, itemsService::noItemsStatusMessage).extractShopItems(),
                this::hasSearchUrl,
                MailKind.SEARCH, searchString));
        if (mode.printsItems() && !outcome.getFound().isEmpty()) {
            log.info("Not sending mail, printing items");
            ItemPrinter.printFound(outcome.getFound(), "found");
        }
    }

    private boolean hasSearchUrl(Shop shop) {
        if (shop.getSearchUrl() == null || shop.getSearchUrl().trim().isEmpty()) {
            log.info("Shop {} has no search URL, skipping", shop.getName());
            return false;
        }
        return true;
    }
}
