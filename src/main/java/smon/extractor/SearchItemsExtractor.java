package smon.extractor;

import org.jsoup.nodes.Document;
import smon.exception.DownloadException;
import smon.message.dto.MessageDto;
import smon.model.Shop;

import java.util.Optional;
import java.util.function.Function;

public class SearchItemsExtractor extends BaseExtractor{

    private String searchString;

    public SearchItemsExtractor(Shop shop, String searchString, Function<String, Optional<MessageDto>> closedShopMessages) {
        super(shop, closedShopMessages);
        this.searchString=searchString;
    }

    @Override
    protected Document downloadDocument() throws DownloadException {
        Document doc = super.getDownloader().getSearchDocument(this.searchString);
        logPage(doc);
        return doc;
    }

}
