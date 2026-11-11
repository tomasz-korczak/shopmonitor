package smon.model;

import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

/**
 * A shop and its selectors. There is deliberately no class-level @Where(disabled = 0): queries that should skip
 * disabled shops say so (see ShopsRepository), and associations (ErrorRecord.shop, Item.shop) always load the shop.
 * The XML import/export format is {@link smon.dto.ShopDefinition}, not this entity.
 */
@Entity
@Table(name="SHOPS")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"listSelectors", "imageSelectors", "linkSelectors", "descriptionSelectors", "priceSelectors", "items"})
public class Shop {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String url;
    /** Informational only (shop list / export); links are resolved against the fetched page. */
    private String baseUrl;
    /** GET URL with a {search} placeholder (see URLUtil). The SEARCH_TYPE column (GET/POST) is not mapped: POST was never implemented. */
    private String searchUrl;
    private Boolean disabled;


    @OneToMany(fetch = FetchType.LAZY, mappedBy = "shop")
    @Where(clause = "selector_type_code = 's_list'")
    @OrderBy("priority ASC")
    @Fetch(FetchMode.SUBSELECT)
    private Set<Selector> listSelectors;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "shop")
    @Where(clause = "selector_type_code = 's_image'")
    @OrderBy("priority ASC")
    @Fetch(FetchMode.SUBSELECT)
    private Set<Selector> imageSelectors;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "shop")
    @Where(clause = "selector_type_code = 's_link'")
    @OrderBy("priority ASC")
    @Fetch(FetchMode.SUBSELECT)
    private Set<Selector> linkSelectors;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "shop")
    @Where(clause = "selector_type_code = 's_desc'")
    @OrderBy("priority ASC")
    @Fetch(FetchMode.SUBSELECT)
    private Set<Selector> descriptionSelectors;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "shop")
    @Where(clause = "selector_type_code = 's_price'")
    @OrderBy("priority ASC")
    @Fetch(FetchMode.SUBSELECT)
    private Set<Selector> priceSelectors;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "shop")
    private List<Item> items;

    public boolean isEnabled() {
        return !Boolean.TRUE.equals(disabled);
    }

    /**
     * Identity is the id (JPA pattern): unsaved shops are equal only to themselves. The hash code is constant, so it
     * does not change when the id is assigned on save and the shop can be used as a map key before and after.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Shop)) {
            return false;
        }
        Long otherId = ((Shop) o).getId();
        return getId() != null && getId().equals(otherId);
    }

    @Override
    public int hashCode() {
        return Shop.class.hashCode();
    }

}
