package smon.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import smon.model.Selector;
import smon.model.Shop;
import smon.shops.service.ShopsService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Command(name = "list", description = "List configured shops")
public class ListShopsCommand extends BaseCommand {

    @Option(name = {"-n", "--name"}, description = "shop name, when supplied lists detailed shop information (also for a disabled shop)")
    private String name;

    @Option(name ={"-d","--details"}, description = "print full details instead of simple shop list (same format as --name)")
    private boolean fullDetails;

    @Option(name = {"-a", "--all"}, description = "also list shops that are not scanned: disabled ones and ones without a list selector")
    private boolean all;

    @Override
    public void run() {
        if (name == null) {
            fullShopListing();
        } else {
            detailedShopList();
        }
    }

    private void fullShopListing() {
        List<Shop> shopList = all ? shopsService.listAllShops() : getShops();
        if (shopList.isEmpty()) {
            System.out.println("No shops configured!");
        } else if (fullDetails) {
            shopList.forEach(shop -> {
                listShopDetails(shop);
                System.out.println();
            });
        } else {
            simpleShopList(shopList);
        }
    }

    private void detailedShopList() {
        Shop shop = shopsService.findShop(name);
        if (shop == null) {
            System.out.printf("Shop %s not found!%n", name);
        } else {
            listShopDetails(shop);
        }
    }

    private void simpleShopList(List<Shop> shopList) {
        int nameWidth = Math.max(4, getMaxLength(shop -> shop.getName().length(), shopList));
        if (all) {
            String format = "%-" + nameWidth + "s %-16s %s%n";
            System.out.printf(format, "Name", "Status", "URL");
            shopList.forEach(shop -> System.out.printf(format, shop.getName(), status(shop), shop.getUrl()));
        } else {
            String format = "%-" + nameWidth + "s %s%n";
            System.out.printf(format, "Name", "URL");
            shopList.forEach(shop -> System.out.printf(format, shop.getName(), shop.getUrl()));
        }
    }

    /** Why a shop is (not) scanned. */
    private static String status(Shop shop) {
        if (!shop.isEnabled()) {
            return "disabled";
        }
        if (shop.getListSelectors() == null || shop.getListSelectors().isEmpty()) {
            return "no list selector";
        }
        return "enabled";
    }

    private void listShopDetails(Shop shop) {
        System.out.printf("%22s: %s%n", "Name", shop.getName());
        System.out.printf("%22s: %s%n", "Id", shop.getId());
        System.out.printf("%22s: %s%n", "Status", status(shop));
        System.out.printf("%22s: %s%n", "URL", shop.getUrl());
        System.out.printf("%22s: %s%n", "Base URL", shop.getBaseUrl());
        System.out.printf("%22s: %s%n", "Search URL", shop.getSearchUrl() == null ? "-" : shop.getSearchUrl());
        System.out.printf("%22s: %s%n", "List selectors", selectors(shop.getListSelectors()));
        System.out.printf("%22s: %s%n", "Description selectors", selectors(shop.getDescriptionSelectors()));
        System.out.printf("%22s: %s%n", "Link selectors", selectors(shop.getLinkSelectors()));
        System.out.printf("%22s: %s%n", "Image selectors", selectors(shop.getImageSelectors()));
        System.out.printf("%22s: %s%n", "Price selectors", selectors(shop.getPriceSelectors()));
    }

    /** One selector per line, in priority order. */
    private static String selectors(Set<Selector> selectors) {
        if (selectors == null || selectors.isEmpty()) {
            return "-";
        }
        return selectors.stream()
                .map(s -> (s.getPriority() == null ? "" : s.getPriority() + ". ") + s.getSelector())
                .collect(Collectors.joining(String.format("%n%24s", "")));
    }
}
