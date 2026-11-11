package smon.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/** Root of the shop import/export XML: {@code <shops><shop>...</shop></shops>}. */
@Data
@NoArgsConstructor
@XmlRootElement(name = "shops")
@XmlAccessorType(XmlAccessType.FIELD)
public class ShopDefinitionList {

    @XmlElement(name = "shop")
    private List<ShopDefinition> shops = new ArrayList<>();

    public ShopDefinitionList(List<ShopDefinition> shops) {
        this.shops = shops;
    }
}
