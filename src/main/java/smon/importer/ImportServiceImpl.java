package smon.importer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smon.dto.ShopDefinition;
import smon.extractor.ExtractionResult;
import smon.extractor.NewItemsExtractor;
import smon.items.service.ItemsService;
import smon.shops.service.ShopsService;

import java.util.List;

@Service
public class ImportServiceImpl implements ImportService {

    private final ShopsService shopsService;
    private final ItemsService itemsService;

    @Autowired
    public ImportServiceImpl(ShopsService shopsService, ItemsService itemsService) {
        this.shopsService = shopsService;
        this.itemsService = itemsService;
    }

    @Override
    public ImportTestResult test(ShopDefinition definition) {
        List<String> problems = definition.validate();
        if (!problems.isEmpty()) {
            return ImportTestResult.invalid(problems);
        }
        boolean nameTaken = shopsService.findShop(definition.getName().trim()) != null;
        ExtractionResult extraction = new NewItemsExtractor(definition.toShop(), itemsService::noItemsStatusMessage)
                .extractShopItems();
        return ImportTestResult.tested(nameTaken, extraction);
    }
}
