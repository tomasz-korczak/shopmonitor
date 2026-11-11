package smon.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;
import lombok.extern.slf4j.Slf4j;
import smon.dto.ShopDefinition;
import smon.dto.ShopDefinitionList;
import smon.dto.ShopItemDto;
import smon.importer.ImportService;
import smon.importer.ImportTestResult;
import smon.model.Shop;
import smon.util.MarshallerUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Command(name = "import", description = "Add shops from an XML file (format: see 'shop export')")
public class ImportCommand extends BaseCommand {

    @Required
    @Option(name = {"-f", "--file"}, description = "XML file with one or more shop definitions. For a template use: shop export --blank")
    private String importFilePath;

    @Option(name = {"-t", "--test"}, description = "test run: download each shop's page and print the extracted items, without saving anything")
    private boolean testRun;

    @Autowired
    private ImportService importService;

    @Override
    public void run() {
        List<ShopDefinition> definitions = readFile();
        if (definitions.isEmpty()) {
            throw new IllegalStateException("No <shop> found in " + importFilePath);
        }
        int failed = 0;
        for (ShopDefinition definition : definitions) {
            if (!(testRun ? testShop(definition) : importShop(definition))) {
                failed++;
            }
        }
        if (failed > 0) {
            throw new IllegalStateException(String.format("%d of %d shops %s", failed, definitions.size(),
                    testRun ? "are not valid" : "were not imported"));
        }
    }

    private List<ShopDefinition> readFile() {
        File file = new File(importFilePath);
        if (!file.isFile()) {
            throw new UsageException("File not found: " + importFilePath);
        }
        try {
            ShopDefinitionList list = MarshallerUtil.unmarshall(file);
            return list.getShops() == null ? Collections.emptyList() : list.getShops();
        } catch (JAXBException e) {
            throw new IllegalStateException("Cannot read " + importFilePath + ": " + (e.getLinkedException() != null ? e.getLinkedException() : e), e);
        }
    }

    private boolean importShop(ShopDefinition definition) {
        try {
            Shop shop = shopsService.addShop(definition);
            System.out.printf("Shop %s imported (id %d)%n", shop.getName(), shop.getId());
            return true;
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    /** Validates and extracts without touching the database; a name that is already taken is reported too. */
    private boolean testShop(ShopDefinition definition) {
        ImportTestResult result = importService.test(definition);
        if (!result.isValid()) {
            log.error("Shop {} is not valid: {}", definition.getName(), String.join(", ", result.getValidationProblems()));
            return false;
        }
        if (result.isNameTaken()) {
            log.warn("Shop {} already exists, importing it would fail", definition.getName());
        }
        Optional<List<ShopItemDto>> extracted = result.getExtraction().getItems();
        if (!extracted.isPresent()) {
            log.error("Shop {}: no items extracted (download failed or the list selector matched nothing)", definition.getName());
            result.getExtraction().getProblems().forEach(problem -> log.error("Shop {}: {}", definition.getName(), problem));
            return false;
        }
        List<ShopItemDto> items = extracted.get();
        ItemPrinter.print(definition.getName(), "items", items);
        long incomplete = items.stream().filter(item -> !item.isComplete()).count();
        if (incomplete > 0) {
            log.warn("Shop {}: {} of {} items have no description or link and would be skipped by scan",
                    definition.getName(), incomplete, items.size());
        }
        return true;
    }

}
