package smon.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShopItemDto {

    private String description;
    private String image;
    private String link;
    /** scale 2, see smon.util.Price */
    private BigDecimal price;

    /** Not a product at all (e.g. a banner or filler tile in the list); dropped silently. */
    public boolean isEmpty() {
        return (description==null || description.length()<2) && price==null ;
    }

    /** A product that can be saved and mailed: it needs a description and a link (the link identifies it). */
    public boolean isComplete() {
        return !isBlank(description) && !isBlank(link);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
