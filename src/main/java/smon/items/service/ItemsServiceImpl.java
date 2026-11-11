package smon.items.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smon.dto.ShopItemDto;
import smon.items.ItemKey;
import smon.items.dao.ItemsRepository;
import smon.message.dto.MessageDto;
import smon.message.service.MessageService;
import smon.model.Item;
import smon.model.Shop;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ItemsServiceImpl implements ItemsService{

    @Autowired
    private ItemsRepository itemsRepository;

    @Autowired
    private MessageService messageService;

    private static final int KEY_LOOKUP_CHUNK = 500;

    @Override
    public Set<String> knownKeys(Collection<String> links) {
        List<String> all = new ArrayList<>(links);
        Set<String> keys = new HashSet<>();
        for (int from = 0; from < all.size(); from += KEY_LOOKUP_CHUNK) {
            List<String> chunk = all.subList(from, Math.min(from + KEY_LOOKUP_CHUNK, all.size()));
            for (Item item : itemsRepository.findByLinkIn(chunk)) {
                keys.add(ItemKey.of(item.getLink(), item.getPrice()));
            }
        }
        return keys;
    }

    @Override
    @Transactional
    public void saveItems(List<ShopItemDto> items, Shop shop, boolean shouldNotify) {
        List<Item> shopItems = items.stream().map( i -> dto2entity(i, shop, shouldNotify)).collect(Collectors.toList());
        itemsRepository.saveAll(shopItems);
    }

    @Override
    public List<Item> getItemsToNotify(Shop shop) {
        return shop == null ? itemsRepository.findAllToNotify() : itemsRepository.findToNotifyByShop(shop.getId());
    }

    @Override
    @Transactional
    public void markNotified(Collection<Long> itemIds) {
        if (itemIds != null && !itemIds.isEmpty()) {
            itemsRepository.clearShouldNotify(itemIds);
        }
    }

    @Override
    public Optional<MessageDto> noItemsStatusMessage(String doc) {
        // A null or blank phrase is skipped: a blank one would match every page.
        return messageService.getAllMessages().stream()
                .filter(m -> m.getMessage() != null && !m.getMessage().trim().isEmpty())
                .filter(m -> doc.contains(m.getMessage()))
                .findFirst();
    }

    private Item dto2entity(ShopItemDto dto, Shop shop, boolean shouldNotify) {
        Item item = new Item();
        item.setDescription(dto.getDescription());
        item.setImage(dto.getImage());
        item.setLink(dto.getLink());
        item.setPrice(dto.getPrice());
        item.setRegistrationDate(LocalDate.now());
        item.setShop(shop);
        item.setShouldNotify(shouldNotify);
        return item;
    }
}
