package smon.scan;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smon.dto.ShopItemDto;
import smon.errors.service.ErrorsService;
import smon.extractor.ExtractionProblem;
import smon.extractor.ExtractionResult;
import smon.items.ItemKey;
import smon.items.service.ItemsService;
import smon.mail.MailKind;
import smon.mail.MailService;
import smon.model.Item;
import smon.model.Shop;
import smon.shops.service.ShopsService;
import smon.util.URLUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScanServiceImpl implements ScanService {

    /** Downloads running at the same time. Shops of one host are never downloaded in parallel (M6). */
    static final int DOWNLOAD_THREADS = 4;

    private final ItemsService itemsService;
    private final ErrorsService errorsService;
    private final MailService mailService;
    private final ShopsService shopsService;

    @Autowired
    public ScanServiceImpl(ItemsService itemsService, ErrorsService errorsService, MailService mailService,
                           ShopsService shopsService) {
        this.itemsService = itemsService;
        this.errorsService = errorsService;
        this.mailService = mailService;
        this.shopsService = shopsService;
    }

    @Override
    public ScanOutcome scan(ScanRequest request) {
        ScanMode mode = request.getMode();
        List<Shop> shops = shopsFor(request.getShopName());
        List<Shop> applicable = shops.stream().filter(request.getApplicable()).collect(Collectors.toList());

        Map<Shop, List<ShopItemDto>> found = new LinkedHashMap<>();
        Downloads downloads = startDownloads(applicable, request);
        try {
            // The database work runs here, one shop at a time and in shop order: the order decides which shop a
            // shared product is attributed to (M3), so it must not depend on which download finished first.
            for (Shop shop : applicable) {
                try {
                    List<ShopItemDto> items = process(shop, downloads.await(shop), mode);
                    if (!items.isEmpty()) {
                        found.put(shop, items);
                    }
                } catch (Exception e) {
                    reportShopFailure(shop, e);
                }
            }
        } finally {
            downloads.pool.shutdown();
        }

        mail(request, shops, found);
        log.info("Scan completed");
        return new ScanOutcome(found);
    }

    private List<Shop> shopsFor(String shopName) {
        if (shopName == null) {
            return shopsService.listShops();
        }
        Shop shop = shopsService.getShopDetails(shopName);
        if (shop == null) {
            throw new IllegalStateException("Shop " + shopName + " not found or disabled! Use 'shop list --all' to list shops.");
        }
        return Collections.singletonList(shop);
    }

    /** Looks at one shop's extraction result: records its problems and returns the items to save or mail. */
    private List<ShopItemDto> process(Shop shop, ExtractionResult result, ScanMode mode) {
        log.info("Shop {} ({}) processing started", shop.getName(), shop.getUrl());
        result.getProblems().forEach(problem -> recordProblem(shop, problem));
        List<ShopItemDto> items = dropIncompleteItems(shop, result.getItems().orElse(Collections.emptyList()));
        if (mode.filtersKnownItems()) {
            items = withoutKnownItems(items);
        }
        if (items.isEmpty()) {
            return items;
        }
        log.info("Shop {}: found {} items", shop.getName(), items.size());
        if (mode.saves()) {
            log.info("Saving shop {} items", shop.getName());
            itemsService.saveItems(items, shop, mode == ScanMode.SAVE_AND_MAIL);
        }
        return items;
    }

    /** One existence query for the shop's links, then an in-memory check (M4). Identity: link plus price. */
    private List<ShopItemDto> withoutKnownItems(List<ShopItemDto> items) {
        List<String> links = items.stream().map(ShopItemDto::getLink).collect(Collectors.toList());
        Set<String> known = itemsService.knownKeys(links);
        return items.stream()
                .filter(item -> !known.contains(ItemKey.of(item.getLink(), item.getPrice())))
                .collect(Collectors.toList());
    }

    /**
     * Items without a description or a link cannot be saved or mailed. They are dropped one by one (the rest of the
     * shop still goes through) and recorded in ERRORS, because it usually means a selector broke.
     */
    private List<ShopItemDto> dropIncompleteItems(Shop shop, List<ShopItemDto> items) {
        List<ShopItemDto> complete = items.stream().filter(ShopItemDto::isComplete).collect(Collectors.toList());
        int dropped = items.size() - complete.size();
        if (dropped > 0) {
            String reason = String.format("%d of %d items without description or link (check selectors)", dropped, items.size());
            log.warn("Shop {}: skipped {}", shop.getName(), reason);
            recordError(() -> errorsService.reportError("Incomplete items in " + shop.getName(), shop, reason));
        }
        return complete;
    }

    private void mail(ScanRequest request, List<Shop> shops, Map<Shop, List<ShopItemDto>> found) {
        switch (request.getMode()) {
            case SAVE_AND_MAIL:
                sendPendingItems(request.getShopName() != null ? shops.get(0) : null);
                break;
            case READ_ONLY_MAIL:
            case SEARCH:
                sendFoundItems(request, found);
                break;
            default:
                // SAVE_NO_MAIL, DRY_RUN and SEARCH_PRINT mail nothing; the command prints the outcome
                break;
        }
    }

    /** Read-only runs and search: mail what this run found. */
    private void sendFoundItems(ScanRequest request, Map<Shop, List<ShopItemDto>> found) {
        if (found.isEmpty()) {
            return;
        }
        log.info("sending mail");
        if (!mailService.sendEmail(request.getMailKind(), found, request.getMailParams())) {
            mailFailed(found.values().stream().mapToInt(List::size).sum());
        }
    }

    /** Normal scan: mail every SHOULD_NOTIFY=true item of the scope (shop == null means all shops). */
    private void sendPendingItems(Shop shop) {
        List<Item> pending = itemsService.getItemsToNotify(shop);
        if (pending.isEmpty()) {
            log.info("No items to notify");
            return;
        }
        Map<Shop, List<ShopItemDto>> mailItems = new LinkedHashMap<>();
        List<Long> itemIds = new ArrayList<>();
        for (Item item : pending) {
            mailItems.computeIfAbsent(item.getShop(), s -> new ArrayList<>()).add(toDto(item));
            itemIds.add(item.getId());
        }
        log.info("sending mail with {} items from {} shops", itemIds.size(), mailItems.size());
        if (mailService.sendEmail(MailKind.SCAN, mailItems)) {
            itemsService.markNotified(itemIds);
        } else {
            mailFailed(itemIds.size());
        }
    }

    private void mailFailed(int itemCount) {
        String message = String.format("Email could not be sent to any recipient (%d items)", itemCount);
        log.error(message);
        recordError(() -> errorsService.reportError(message, null, "all recipients failed, see log file"));
        throw new IllegalStateException(message);
    }

    private void reportShopFailure(Shop shop, Exception e) {
        log.error("Shop {} processing failed: {}", shop.getName(), e.toString());
        log.debug("Shop processing failed", e);
        recordError(() -> errorsService.reportError("Exception during processing " + shop.getName(), shop, e));
    }

    private void recordProblem(Shop shop, ExtractionProblem problem) {
        recordError(() -> {
            if (problem.getCause() != null) {
                errorsService.reportError(problem.getMessage(), shop, problem.getCause());
            } else {
                errorsService.reportError(problem.getMessage(), shop, problem.getReason());
            }
        });
    }

    /** Recording an error must never break the run itself. */
    private void recordError(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("Cannot record error in ERRORS table: {}", e.toString());
        }
    }

    /**
     * Starts the downloads: one task per host, which runs that host's shops one after the other, so a single site
     * never gets parallel requests (M6). The results are collected by {@link Downloads#await(Shop)}.
     */
    private Downloads startDownloads(List<Shop> shops, ScanRequest request) {
        Downloads downloads = new Downloads(Executors.newFixedThreadPool(DOWNLOAD_THREADS));
        Map<String, List<Shop>> byHost = new LinkedHashMap<>();
        for (Shop shop : shops) {
            String target = request.getMode().isSearch() ? shop.getSearchUrl() : shop.getUrl();
            String host = URLUtil.getBaseUrl(target);
            byHost.computeIfAbsent(host == null ? "" : host, h -> new ArrayList<>()).add(shop);
        }
        for (List<Shop> hostShops : byHost.values()) {
            Future<?> task = downloads.pool.submit(() -> {
                for (Shop shop : hostShops) {
                    downloads.results.put(shop, extractSafely(shop, request.getExtractor()));
                }
            });
            for (Shop shop : hostShops) {
                downloads.tasks.put(shop, task);
            }
        }
        return downloads;
    }

    /** A failure in the extraction is one shop's problem, not the run's. */
    private static ExtractionResult extractSafely(Shop shop, Function<Shop, ExtractionResult> extractor) {
        try {
            return extractor.apply(shop);
        } catch (RuntimeException e) {
            return ExtractionResult.failed(Collections.singletonList(
                    new ExtractionProblem("Exception during processing " + shop.getName(), null, e)));
        }
    }

    private static ShopItemDto toDto(Item item) {
        ShopItemDto dto = new ShopItemDto();
        dto.setDescription(item.getDescription());
        dto.setImage(item.getImage());
        dto.setLink(item.getLink());
        dto.setPrice(item.getPrice());
        return dto;
    }

    /** The running downloads and their results, one entry per shop. */
    private static final class Downloads {

        final ExecutorService pool;
        /** Written by the main thread only. */
        final Map<Shop, Future<?>> tasks = new HashMap<>();
        /** Written by the download threads, read by the main thread after the task finished. */
        final Map<Shop, ExtractionResult> results = new ConcurrentHashMap<>();

        Downloads(ExecutorService pool) {
            this.pool = pool;
        }

        ExtractionResult await(Shop shop) {
            try {
                tasks.get(shop).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for " + shop.getName(), e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Download failed for " + shop.getName(), e.getCause());
            }
            return results.get(shop);
        }
    }
}
