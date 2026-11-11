package smon.cli;

import org.springframework.beans.factory.annotation.Autowired;
import smon.model.Shop;
import smon.selectors.service.SelectorsService;
import smon.shops.service.ShopsService;

import java.util.List;
import java.util.function.Function;

/**
 * Commands are created by airline, not by Spring; Application.run fills their @Autowired fields
 * (autowireBean) before running them.
 */
public abstract class BaseCommand implements Runnable {

    @Autowired
    protected ShopsService shopsService;

    @Autowired
    protected SelectorsService selectorsService;

    protected Integer getMaxLength(Function<Shop, Integer> function, List<Shop> shops) {
        Integer max = 0;
        for (Shop shop : shops) {
            Integer value = function.apply(shop);
            if (max.compareTo(value) < 0) {
                max = value;
            }
        }
        return max;
    }

    protected List<Shop> getShops() {
        return shopsService.listShops();
    }

    protected List<String> getAllSelectorsByCode(String code) {
        return selectorsService.getAllSelectors(code);
    }
}
