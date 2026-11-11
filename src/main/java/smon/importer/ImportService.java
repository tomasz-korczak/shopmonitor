package smon.importer;

import smon.dto.ShopDefinition;

/** Imports shop definitions. */
public interface ImportService {

    /** Validates the definition, checks the name and downloads the page. Saves nothing. */
    ImportTestResult test(ShopDefinition definition);
}
