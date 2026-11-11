package smon.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import smon.dto.ShopDefinition;
import smon.dto.ShopDefinitionList;
import smon.model.Shop;
import smon.shops.service.ShopsService;
import smon.util.MarshallerUtil;

import javax.xml.bind.JAXBException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Command(name = "export", description = "Export shop definitions as XML (the 'shop import' format), also disabled shops")
public class ExportCommand extends BaseCommand{

    @Option(name = {"-f", "--file"}, description = "output file (default: standard output)")
    private String exportFilePath;

    @Option(name = {"-b", "--blank"}, description = "write a template for a new shop instead")
    private boolean blank;

    @Option(name = {"-n", "--name"}, description = "export only this shop (default: all shops)")
    private String shopName;

    @Override
    public void run() {
        if (blank && shopName != null) {
            throw new UsageException("use either --blank or --name, not both");
        }
        ShopDefinitionList list = new ShopDefinitionList(blank ? Collections.singletonList(template()) : definitions());
        try (OutputStream out = exportFilePath == null ? nonClosing(System.out) : new FileOutputStream(exportFilePath)) {
            MarshallerUtil.marshall(list, out);
        } catch (IOException | JAXBException e) {
            throw new IllegalStateException("Export failed: " + e, e);
        }
        if (exportFilePath != null) {
            System.out.printf("%d shops exported to %s%n", list.getShops().size(), exportFilePath);
        }
    }

    private List<ShopDefinition> definitions() {
        List<Shop> shops;
        if (shopName == null) {
            shops = shopsService.listAllShops();
        } else {
            Shop shop = shopsService.findShop(shopName);
            if (shop == null) {
                throw new IllegalStateException("Shop " + shopName + " not found! Use 'shop list --all' to list shops.");
            }
            shops = Collections.singletonList(shop);
        }
        return shops.stream().map(ShopDefinition::of).collect(Collectors.toList());
    }

    /**
     * A shop with example values that pass {@link ShopDefinition#validate()}. The optional searchUrl may use
     * {search}, {search+} or {search,}; the second list selector is a fallback, so it is left out of the template.
     */
    static ShopDefinition template() {
        ShopDefinition template = new ShopDefinition("shop-name", "https://shop.example.pl/new-products");
        template.setSearchUrl("https://shop.example.pl/search?q={search}");
        template.setDisabled(false);
        template.setListSelectors(Collections.singletonList("#products > div.product"));
        template.setDescriptionSelectors(Collections.singletonList("h3.name"));
        template.setLinkSelectors(Collections.singletonList("a.product-link"));
        template.setImageSelectors(Collections.singletonList("img"));
        template.setPriceSelectors(Collections.singletonList("span.price"));
        return template;
    }

    /** System.out must stay open after the export. */
    private static OutputStream nonClosing(OutputStream out) {
        return new java.io.FilterOutputStream(out) {
            @Override
            public void close() throws IOException {
                flush();
            }
        };
    }
}
