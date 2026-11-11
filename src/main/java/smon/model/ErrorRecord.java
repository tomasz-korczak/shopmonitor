package smon.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="ERRORS")
@Getter
@Setter
@ToString(exclude = "shop")
@AllArgsConstructor
@NoArgsConstructor
public class ErrorRecord {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String errorMessage;
    private String reason;
    private String sourceClass;
    private LocalDate registrationDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="SHOP_ID")
    private Shop shop;

    /** Identity is the id (see {@link Shop#equals(Object)}). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ErrorRecord)) {
            return false;
        }
        return getId() != null && getId().equals(((ErrorRecord) o).getId());
    }

    @Override
    public int hashCode() {
        return ErrorRecord.class.hashCode();
    }
}
