package smon.cli;

import smon.dto.ShopItemDto;
import smon.model.Shop;
import smon.util.Price;

import java.util.List;
import java.util.Map;

/** Prints extracted items to stdout: a shop header, then price / description / link per item. */
final class ItemPrinter {

    private ItemPrinter() {
    }

    /** The items a scan or search found, one block per shop (used when nothing is mailed). */
    static void printFound(Map<Shop, List<ShopItemDto>> found, String label) {
        found.forEach((shop, items) -> print(shop.getName(), label, items));
    }

    static void print(String shopName, String label, List<ShopItemDto> items) {
        System.out.printf("%n%s (%d %s)%n", shopName, items.size(), label);
        for (ShopItemDto item : items) {
            String price = item.getPrice() == null ? "-" : Price.format(item.getPrice());
            System.out.printf("  %12s  %s%n  %12s  %s%n", price, item.getDescription(), "", item.getLink());
        }
    }
}
