package smon.shops.service;

import smon.dto.ShopDefinition;
import smon.model.Shop;

import java.util.List;

public interface ShopsService {

    /** Shops that are scanned: enabled, with a list selector. */
    List<Shop> listShops();

    /** A shop that is scanned (see {@link #listShops()}), or null. */
    Shop getShopDetails(String shopName);

    /** Every shop, also disabled ones and ones without a list selector, with all selectors loaded. */
    List<Shop> listAllShops();

    /** Any shop by name (also disabled), with all selectors loaded, or null. */
    Shop findShop(String shopName);

    /**
     * Saves a new shop with its selectors in one transaction.
     * @throws IllegalArgumentException when the definition is invalid or the name is taken
     */
    Shop addShop(ShopDefinition definition);
}
