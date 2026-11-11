package smon.scan;

/**
 * What a scan command does with the items it finds. The commands map their options to a mode; the service decides
 * what is saved, mailed and reported.
 */
public enum ScanMode {
    /** scan: new items are saved with SHOULD_NOTIFY=true, then every pending item of the scope is mailed. */
    SAVE_AND_MAIL,
    /** scan -ro: nothing is saved, the new items found in this run are mailed. */
    READ_ONLY_MAIL,
    /** scan -nm: new items are saved with SHOULD_NOTIFY=false, nothing is mailed; the items are printed. */
    SAVE_NO_MAIL,
    /** scan -ro -nm: nothing is saved or mailed; the new items are printed. */
    DRY_RUN,
    /** search: every match on the search page is mailed, including items already stored (H1); nothing is saved. */
    SEARCH,
    /** search -nm: every match is printed; nothing is saved or mailed. */
    SEARCH_PRINT;

    /** The scan commands' options: -ro (read-only) and -nm (no mail). */
    public static ScanMode forScan(boolean readOnly, boolean noMail) {
        if (readOnly) {
            return noMail ? DRY_RUN : READ_ONLY_MAIL;
        }
        return noMail ? SAVE_NO_MAIL : SAVE_AND_MAIL;
    }

    /** The search command's option: -nm (no mail). */
    public static ScanMode forSearch(boolean noMail) {
        return noMail ? SEARCH_PRINT : SEARCH;
    }

    /** Whether the items are looked up in ITEMS, so that only new ones are kept. Search reports every match. */
    public boolean filtersKnownItems() {
        return this == SAVE_AND_MAIL || this == READ_ONLY_MAIL || this == SAVE_NO_MAIL || this == DRY_RUN;
    }

    /** Whether new items are written to ITEMS. */
    public boolean saves() {
        return this == SAVE_AND_MAIL || this == SAVE_NO_MAIL;
    }

    /** Whether the command prints the items on stdout. */
    public boolean printsItems() {
        return this == SAVE_NO_MAIL || this == DRY_RUN || this == SEARCH_PRINT;
    }

    public boolean isSearch() {
        return this == SEARCH || this == SEARCH_PRINT;
    }
}
