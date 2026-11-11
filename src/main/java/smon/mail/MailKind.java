package smon.mail;

import lombok.Getter;

@Getter
public enum MailKind {
    SCAN("Nowe pozycje w sklepach:"),
    SEARCH("Wynik wyszukiwania <span style='color:red'>%s</span>:");

    private String title;

    MailKind(String title) {
        this.title = title;
    }

}
