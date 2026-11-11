package smon.extractor;

import smon.dto.ShopItemDto;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * What an extraction produced.
 * <ul>
 *   <li>{@link #getItems()} is empty when the page could not be read or no list selector matched (see
 *       {@link #getProblems()}); it is present, possibly as an empty list, when the page was parsed;</li>
 *   <li>{@link #getProblems()} lists what went wrong, for the caller to record or print.</li>
 * </ul>
 */
public final class ExtractionResult {

    private final Optional<List<ShopItemDto>> items;
    private final List<ExtractionProblem> problems;

    private ExtractionResult(Optional<List<ShopItemDto>> items, List<ExtractionProblem> problems) {
        this.items = items;
        this.problems = Collections.unmodifiableList(problems);
    }

    /** The page was parsed; the list may be empty. */
    public static ExtractionResult parsed(List<ShopItemDto> items) {
        return new ExtractionResult(Optional.of(items), Collections.emptyList());
    }

    /** Nothing could be extracted; the problems say why. */
    public static ExtractionResult failed(List<ExtractionProblem> problems) {
        return new ExtractionResult(Optional.empty(), problems);
    }

    public Optional<List<ShopItemDto>> getItems() {
        return items;
    }

    public List<ExtractionProblem> getProblems() {
        return problems;
    }
}
