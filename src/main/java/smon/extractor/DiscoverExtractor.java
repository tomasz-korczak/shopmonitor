package smon.extractor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import smon.exception.DownloadException;

@Slf4j
@Getter
public class DiscoverExtractor extends BaseExtractor {

    private String url;
    private Document doc;

    public DiscoverExtractor(String url) throws DownloadException {
        this.url=url;
        this.doc = downloadDocument();
    }

    /** For a page that is already downloaded, e.g. a saved page in the tests. */
    public DiscoverExtractor(Document doc) {
        this.url = doc.location();
        this.doc = doc;
    }

    @Override
    protected Document downloadDocument() throws DownloadException {
        Document doc = super.getDownloader().getDocument(url);
        logPage(doc);
        return doc;
    }


}
