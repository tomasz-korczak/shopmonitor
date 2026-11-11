package smon.extractor;

import org.jsoup.nodes.Document;
import smon.exception.DownloadException;
import smon.message.dto.MessageDto;
import smon.model.Shop;

import java.util.Optional;
import java.util.function.Function;

public class NewItemsExtractor extends BaseExtractor{

    public NewItemsExtractor(Shop shop, Function<String, Optional<MessageDto>> closedShopMessages) {
        super(shop, closedShopMessages);
    }

    @Override
    protected Document downloadDocument() throws DownloadException {
        Document doc = super.getDownloader().getDocument();
        logPage(doc);
        return doc;
    }

}
