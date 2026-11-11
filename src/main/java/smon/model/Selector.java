package smon.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

/**
 * One selector of a shop. The type is kept as the string column selectorTypeCode (s_list, s_desc, s_image, s_link,
 * s_price); nothing needs the SELECTOR_TYPES row, so there is no association to it.
 */
@Entity
@Table(name = "SELECTORS")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"id", "shop"})
public class Selector {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String selector;
    private Integer priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = true, updatable = false)
    private Shop shop;

    @Column(name="selector_type_code")
    private String selectorTypeCode;

    /** Identity is the id; see {@link Shop#equals(Object)}. The shop is not part of it, so it is never loaded for equality. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Selector)) {
            return false;
        }
        return getId() != null && getId().equals(((Selector) o).getId());
    }

    @Override
    public int hashCode() {
        return Selector.class.hashCode();
    }
}
