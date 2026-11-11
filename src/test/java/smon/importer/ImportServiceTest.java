package smon.importer;

import org.junit.jupiter.api.Test;
import smon.dto.ShopDefinition;
import smon.items.service.ItemsService;
import smon.shops.service.ShopsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ImportServiceTest {

    /** Only the validation path: a valid definition would download its page, which a unit test must not do. */
    @Test
    void invalidDefinitionIsReportedWithoutDownloadingOrCheckingTheName() {
        ShopsService shops = mock(ShopsService.class);
        ItemsService items = mock(ItemsService.class);

        ImportTestResult result = new ImportServiceImpl(shops, items).test(new ShopDefinition("", "not a url"));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getValidationProblems()).anyMatch(problem -> problem.startsWith("name is missing"));
        assertThat(result.getExtraction()).isNull();
        verify(shops, never()).findShop(anyString());
    }
}
