package smon.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;
import smon.discover.DiscoverResult;
import smon.discover.DiscoverService;
import smon.exception.DownloadException;
import smon.extractor.DiscoverExtractor;
import smon.model.Shop;
import smon.util.URLUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Command(name = "discover", description = "Discover new shop configuration")
public class DiscoverCommand extends BaseCommand {

    @Required
    @Option(name = {"-u", "--url"}, description = "Shop items url")
    private String url;

    @Option(name = {"-a", "--add"}, description = "Add the shop; refused unless list, description and link selectors were discovered")
    private boolean add;

    @Option(name = {"-n", "--name"}, description = "New shop's name, required with -a")
    private String name;

    private final DiscoverService discoverService = new DiscoverService();

    @Override
    public void run() {
        if (add) {
            if (name == null || name.trim().isEmpty()) {
                throw new UsageException("-a needs the new shop's name: -n <name>");
            }
            if (shopsService.findShop(name.trim()) != null) {
                throw new IllegalStateException("Shop " + name + " already exists");
            }
        }
        try {
            DiscoverExtractor page = new DiscoverExtractor(url);
            Map<String, List<String>> stored = new HashMap<>();
            for (String type : DiscoverService.ALL_TYPES) {
                stored.put(type, getAllSelectorsByCode(type));
            }
            DiscoverResult result = discoverService.discover(page, stored);
            printResults(url, result);

            if (add) {
                Shop shop = shopsService.addShop(discoverService.toDefinition(name, url, result));
                System.out.printf("Shop %s saved (id %d)%n", shop.getName(), shop.getId());
            }
        } catch (DownloadException e) {
            throw new IllegalStateException("Cannot download " + url + ": " + e.getOriginalException(), e);
        }
    }

    private void printResults(String url, DiscoverResult result) {
        System.out.format("%8s:%s%n", "URL", url);
        System.out.format("%8s:%s%n", "Base URL", URLUtil.getBaseUrl(url));

        for (String type : result.getSelectors().keySet()) {
            System.out.format("%8s:%n", type);
            for (String selector : result.getSelectors().get(type)) {
                System.out.format("%10s%s%n", "", selector);
            }
        }
        List<String> lackingSelectors = result.getLacking();
        if (!lackingSelectors.isEmpty()) {
            System.out.println("Lacking following selectors: " + lackingSelectors);
        } else {
            System.out.println("All selectors found ;)");
        }
    }
}
