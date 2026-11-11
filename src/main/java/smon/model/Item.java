package smon.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="ITEMS")
@Getter
@Setter
@ToString(exclude = "shop")
@AllArgsConstructor
@NoArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String description;
    private String image;
    private String link;
    /** DECIMAL(10,2) in ITEMS; the column already has this type, the annotation documents it (ddl-auto=none). */
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    /** DATE in ITEMS, like ErrorRecord.registrationDate. */
    private LocalDate registrationDate;

    /** true = not mailed yet; cleared once an email containing the item reached at least one recipient */
    private Boolean shouldNotify;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="SHOP_ID")
    private Shop shop;

    /** Identity is the id (see {@link Shop#equals(Object)}). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Item)) {
            return false;
        }
        return getId() != null && getId().equals(((Item) o).getId());
    }

    @Override
    public int hashCode() {
        return Item.class.hashCode();
    }
}
