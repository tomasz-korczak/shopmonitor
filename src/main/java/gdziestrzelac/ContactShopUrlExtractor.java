package gdziestrzelac;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ContactShopUrlExtractor {

    private static final String INPUT_FILE = "c:\\temp\\addr.txt";

    public static void main(String[] args) {
        SslUtil.trustAllCertificates();
        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String url = line.trim();

                // pomijamy puste linie i linie nie będące URL
                if (!isHttpUrl(url)) {
                    continue;
                }

                processUrl(url);
            }

        } catch (IOException e) {
            System.err.println("Błąd odczytu pliku: " + e.getMessage());
        }
    }

    private static void processUrl(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();

            // element musi mieć WSZYSTKIE klasy
            Elements contactBoxes = doc.select(
                    ".column.mcb-column.one-third.column_contact_box"
            );

            for (Element box : contactBoxes) {
                // szukamy <a> z atrybutem target
                Element link = box.selectFirst("a[target]");

                if (link != null) {
                    String href = link.attr("href");
                    System.out.println(href);
                }
            }

        } catch (IOException e) {
            System.err.println("Błąd pobierania strony: " + url + " (" + e.getMessage() + ")");
        }
    }

    private static boolean isHttpUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }
}
