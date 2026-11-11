package smon.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "SELECTOR_TYPES")
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString()
public class SelectorType {
    @Id
    private String code;
    private String name;
}
