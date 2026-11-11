package smon.scan;

import smon.dto.ShopItemDto;
import smon.model.Shop;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** The items a scan found in this run (after the known-item filter), per shop, in shop order. */
public final class ScanOutcome {

    private final Map<Shop, List<ShopItemDto>> found;

    public ScanOutcome(Map<Shop, List<ShopItemDto>> found) {
        this.found = Collections.unmodifiableMap(found);
    }

    public Map<Shop, List<ShopItemDto>> getFound() {
        return found;
    }
}
