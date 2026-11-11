package smon.importer;

import smon.extractor.ExtractionResult;

import java.util.Collections;
import java.util.List;

/** Outcome of a test run of a shop definition (shop import -t): nothing is saved. */
public final class ImportTestResult {

    private final List<String> validationProblems;
    private final boolean nameTaken;
    private final ExtractionResult extraction;

    private ImportTestResult(List<String> validationProblems, boolean nameTaken, ExtractionResult extraction) {
        this.validationProblems = validationProblems;
        this.nameTaken = nameTaken;
        this.extraction = extraction;
    }

    /** The definition is not valid; nothing was extracted. */
    public static ImportTestResult invalid(List<String> validationProblems) {
        return new ImportTestResult(validationProblems, false, null);
    }

    /** The definition is valid and the page was extracted. */
    public static ImportTestResult tested(boolean nameTaken, ExtractionResult extraction) {
        return new ImportTestResult(Collections.emptyList(), nameTaken, extraction);
    }

    public boolean isValid() {
        return validationProblems.isEmpty();
    }

    public List<String> getValidationProblems() {
        return validationProblems;
    }

    /** True when a shop with this name already exists, so importing it would fail. */
    public boolean isNameTaken() {
        return nameTaken;
    }

    /** The extraction; null when the definition is not valid. */
    public ExtractionResult getExtraction() {
        return extraction;
    }
}
