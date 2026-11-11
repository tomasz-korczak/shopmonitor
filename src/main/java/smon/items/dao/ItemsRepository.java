package smon.items.dao;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import smon.model.Item;

import java.util.Collection;
import java.util.List;

public interface ItemsRepository extends CrudRepository<Item, Long> {

    List<Item> findByLinkIn(Collection<String> links);

    @Query("select i from Item i join fetch i.shop where i.shouldNotify = true order by i.id")
    List<Item> findAllToNotify();

    @Query("select i from Item i join fetch i.shop s where i.shouldNotify = true and s.id = :shopId order by i.id")
    List<Item> findToNotifyByShop(@Param("shopId") Long shopId);

    @Modifying
    @Query("update Item i set i.shouldNotify = false where i.id in :ids")
    int clearShouldNotify(@Param("ids") Collection<Long> ids);
}
