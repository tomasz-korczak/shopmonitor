package smon.mail;

import smon.dto.ShopItemDto;
import smon.model.Shop;
import smon.util.Price;

import java.util.*;

public class MailUtil {

    private final Map<Shop, List<ShopItemDto>> items;
    private final String subject;
    /** email.footer.unsubscribe-to; no unsubscribe line when blank */
    private final String unsubscribeTo;
    /** email.footer.copyright; no copyright line when blank */
    private final String copyright;

    public MailUtil(Map<Shop, List<ShopItemDto>> items, String subject,
                    String unsubscribeTo, String copyright) {
        this.items = items;
        this.subject = subject;
        this.unsubscribeTo = unsubscribeTo;
        this.copyright = copyright;
    }

    public String formatSubject() {
        Date now = new Date();
        return String.format("%s %tY-%tm-%td %tH:%tM", subject, now, now, now, now, now);
    }

    public String formatHTMLMessage(MailKind mailKind, String ... params) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><head><style>" +
            "       BODY {"+
            "           font-family: Arial;\n"+
                "           font-size: 14px;\n"+
            "       }\n"+

            "       IMG {"+
            "           border: 0px solid #ddd;\n"+
            "           border-radius: 4px;\n"+
            "           padding: 5px;\n"+
            "           max-width: 100px;\n"+
            "           height: auto;\n"+
            "       }\n"+

		    "       .header {"+
            "       }\n"+

            "       .tabelka {"+
            "           border-collapse: collapse;\n"+
            "           margin: 25px 0;\n"+
            "           font-size: 1em;\n"+
            "           font-family: sans-serif;\n"+
            "           border-radius: 5px 5px 0 0;\n"+
            "           overflow: hidden;\n"+
            "           box-shadow: 0 0 20px rgba(0, 0, 0, 0.15);\n"+
            "       }\n"+

            "       .tabelka thead tr {"+
            "           background-color: #009879;\n"+
            "           color: #ffffff;\n"+
            "           text-align: left;\n"+
            "           font-size: 16px;\n"+
            "       }\n"+

            "       .tabelka th, .tabelka td {"+
            "           padding: 12px 15px;\n"+
            "       }\n"+

            "       .tabelka td{"+
            "           display: -webkit-flex;\n"+
            "           display: -ms-flexbox;\n"+
            "           display: flex;\n"+
            "           flex-wrap: wrap;\n"+
            "           overflow: hidden;\n"+
            "       }\n"+

            "       .tabelka tbody tr {"+
            "           border-bottom: 1px solid #dddddd;\n"+
            "       }\n"+

            "       .tabelka tbody tr:nth-of-type(even) {"+
            "           background-color: #f3f3f3;\n"+
            "       }\n"+

            "       .tabelka tbody tr:last-of-type {"+
            "           border-bottom: 2px solid #009879;\n"+
            "       }\n"+

            "       .tabelka tbody tr.active-row {"+
            "           font-weight: bold;\n"+
            "           color: #009879;\n"+
            "       }\n"+

            "       DIV.box {"+
            "           margin: 0 5px 5px 0;\n"+
            "           padding: 0;\n"+
            "           width: 200px;\n"+
            "           text-align: center;\n"+
            "           line-height: 18px;\n"+
            "           border: 1px solid #ddd;\n"+
            "           border-radius: 4px;\n"+
            "           vertical-align:top;\n"+
            "           background-color: #FAFAFA;\n"+
            "           overflow: hidden;\n"+
            "       }\n"+

            "       A {"+
            "           color: black;\n"+
            "       }\n"+

            "       a:link { text-decoration: none; }\n"+
            "       a:visited { text-decoration: none; }\n"+
            "       a:hover { text-decoration: none; }\n"+
            "       a:active { text-decoration: none; }\n"+

            "       A.image {"+
            "           display: block;\n"+
            "       }\n"+

            "       A.desc {"+
            "           display: block;\n"+
            "       }\n"+

            "       SPAN.price {"+
            "           display: block;\n"+
            "           color: red;\n"+
            "       }\n"+
            "    P.footer {\n"+
            "       width:100%;\n"+
            "       display: flex;\n"+
            "          justify-content: center; }\n"+
            "   A.footer { color:blue; text-decoration: underline;}\n"+
            "   P.copy {\n"+
            "       width:100%;\n"+
            "       display: flex;\n"+
            "       justify-content: center; }\n"+
            "    </style>\n" +
                "</head>\n");

        builder.append("<body>\n");
        builder.append(formatHTMLContent(mailKind, params));
        if (unsubscribeTo != null && !unsubscribeTo.trim().isEmpty()) {
            builder.append("<p class='footer'>Jeśli nie chcesz już otrzymywać mailingu&nbsp;<a class='footer' href='mailto:")
                    .append(escape(unsubscribeTo.trim()))
                    .append("?subject=Wypisz%20mnie%20z%20shopmonitora'>wypisz się tu</a></p>");
        }
        if (copyright != null && !copyright.trim().isEmpty()) {
            builder.append("<p class='copy'>").append(escape(copyright.trim())).append("</p>");
        }
        builder.append("</body>\n");
        builder.append("</html>\n");
        return builder.toString();
    }

    private String formatHTMLContent(MailKind mailKind, String ... params) {
        StringBuilder builder = new StringBuilder();
        String title = decodeTitle(mailKind, params);
        builder.append("<span style='font-weight: bold; font-size: 18px;'>"+title+"</span><br><br>");
        items.keySet().stream().sorted(Comparator.comparing(Shop::getName)).forEach(key -> {
            builder.append("<table class='tabelka'>");
            builder.append("<thead><tr><th><span>Sklep: <a href='"+escape(key.getUrl())+"'>"+escape(key.getName())+"</a>");
            builder.append(" nowych pozycji: "+items.get(key).size());
            builder.append("</span></th></tr></thead>");
            builder.append("<tbody><tr><td>");
            items.get(key).stream().forEach(item -> {
                builder.append("<div class=\"box\">");
                builder.append("<a class=\"image\" style='vertical-align: middle;' href='"+escape(item.getLink())+"'><img src='"+escape(item.getImage())+"'/></a>");
                builder.append("<a class=\"desc\" style='vertical-align: middle;' href='"+escape(item.getLink())+"'>"+escape(item.getDescription())+"</a>");
                builder.append("<span class=\"price\">").append(escape(Price.format(item.getPrice()))).append("</span>");
                builder.append("</div>");
            });
            builder.append("</td></tr></tbody></table>");
        });
        return builder.toString();
    }

    private String decodeTitle(MailKind mailKind, String... params) {
        String title = mailKind.getTitle();
        if (params != null && params.length>0) {
            Object[] escaped = Arrays.stream(params).map(MailUtil::escape).toArray();
            title = String.format(title, escaped);
        }
        return title;
    }

    /** HTML-escapes scraped/user text for use in element content and in single- or double-quoted attributes. */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

}
