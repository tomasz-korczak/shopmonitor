package smon.items.service;

import smon.dto.ShopItemDto;
import smon.message.dto.MessageDto;
import smon.model.Item;
import smon.model.Shop;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ItemsService {

    /**
     * The identities (see {@link smon.items.ItemKey}) of the stored items with one of the given links, of any shop.
     * Done in chunks, so one query covers a shop's whole list (M4).
     */
    Set<String> knownKeys(Collection<String> links);

    void saveItems(List<ShopItemDto> items, Shop shop, boolean shouldNotify);

    /** Items with SHOULD_NOTIFY=true (shop join-fetched); limited to the given shop, or all shops when shop is null. */
    List<Item> getItemsToNotify(Shop shop);

    /** Sets SHOULD_NOTIFY=false for the given item ids. */
    void markNotified(Collection<Long> itemIds);

    Optional<MessageDto> noItemsStatusMessage(String doc);
}
