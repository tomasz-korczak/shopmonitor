package smon.selectors.service;

import smon.model.Selector;

import java.util.Collection;
import java.util.List;

public interface SelectorsService {
    List<String> getAllSelectors(String code);

    /** Saves selectors whose shop is already saved. */
    void saveSelectors(Collection<Selector> selectors);
}
