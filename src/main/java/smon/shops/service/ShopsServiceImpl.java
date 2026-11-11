package smon.shops.service;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smon.dto.ShopDefinition;
import smon.selectors.service.SelectorsService;
import smon.shops.dao.ShopsRepository;
import smon.model.Shop;

import java.util.List;

@Service
public class ShopsServiceImpl implements ShopsService {

    @Autowired
    private ShopsRepository shopsRepository;

    @Autowired
    private SelectorsService selectorService;

    @Override
    @Transactional(readOnly = true)
    public List<Shop> listShops() {
        return initialized(shopsRepository.getShops(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Shop getShopDetails(String shopName) {
        List<Shop> shops = initialized(shopsRepository.getShops(shopName));
        return shops.isEmpty() ? null : shops.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shop> listAllShops() {
        return initialized(shopsRepository.getAllShops(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Shop findShop(String shopName) {
        List<Shop> shops = initialized(shopsRepository.getAllShops(shopName));
        return shops.isEmpty() ? null : shops.get(0);
    }

    /**
     * The selector collections are fetched by subselects, so they are loaded here, while the transaction is open.
     * The shops are then used after it ends (and by download threads), where a lazy load would fail.
     */
    private static List<Shop> initialized(List<Shop> shops) {
        for (Shop shop : shops) {
            Hibernate.initialize(shop.getListSelectors());
            Hibernate.initialize(shop.getImageSelectors());
            Hibernate.initialize(shop.getLinkSelectors());
            Hibernate.initialize(shop.getDescriptionSelectors());
            Hibernate.initialize(shop.getPriceSelectors());
        }
        return shops;
    }

    @Override
    @Transactional
    public Shop addShop(ShopDefinition definition) {
        List<String> problems = definition.validate();
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("Shop " + definition.getName() + " is not valid: " + String.join(", ", problems));
        }
        if (shopsRepository.existsByName(definition.getName().trim())) {
            throw new IllegalArgumentException("Shop " + definition.getName() + " already exists");
        }
        Shop shop = definition.toShop();
        try {
            shopsRepository.save(shop);
        } catch (DataIntegrityViolationException e) {
            // the check above is not atomic: a concurrent import of the same name is refused by the unique index on SHOPS.NAME
            throw new IllegalArgumentException("Shop " + definition.getName() + " already exists", e);
        }
        selectorService.saveSelectors(ShopDefinition.allSelectors(shop));
        return shop;
    }

}
