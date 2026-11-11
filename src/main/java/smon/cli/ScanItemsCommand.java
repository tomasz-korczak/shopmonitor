package smon.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import smon.extractor.NewItemsExtractor;
import smon.items.service.ItemsService;
import smon.mail.MailKind;
import smon.scan.ScanMode;
import smon.scan.ScanOutcome;
import smon.scan.ScanRequest;
import smon.scan.ScanService;

@Slf4j
@Command(name = "scan", description = "Scanning shop webpage for items")
public class ScanItemsCommand extends BaseCommand {

    @Option(name = {"-n", "--name"}, description = "shop name, when supplied limits scanning to this shop only")
    private String shopName;

    @Option(name = {"-ro", "--read-only"}, description = "do not save new items (will be reported again on next run)")
    private boolean readOnly;

    @Option(name = {"-nm", "--no-mail"}, description = "do not send email report; new items are saved as already notified and will never be mailed")
    private boolean noMail;

    @Autowired
    private ScanService scanService;

    @Autowired
    private ItemsService itemsService;

    @Override
    public void run() {
        ScanMode mode = ScanMode.forScan(readOnly, noMail);
        ScanOutcome outcome = scanService.scan(new ScanRequest(shopName, mode,
                shop -> new NewItemsExtractor(shop, itemsService::noItemsStatusMessage).extractShopItems(),
                shop -> true,
                MailKind.SCAN));
        if (mode.printsItems() && !outcome.getFound().isEmpty()) {
            log.info("Not sending mail, printing items");
            ItemPrinter.printFound(outcome.getFound(), "new");
        }
    }
}
