package smon.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="MESSAGES")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    @Id
    private Long id;
    private String message;
    private String type;

}
