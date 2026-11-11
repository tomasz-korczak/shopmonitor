package smon.shops.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import smon.model.Shop;

import java.util.List;

/**
 * Shops are loaded without joins: a join fetch of the five selector collections would return one row per combination
 * of selectors. The collections are fetched by subselects instead (see {@link smon.model.Shop}); ShopsServiceImpl
 * initialises them inside its transaction.
 */
@Repository
public interface ShopsRepository extends JpaRepository<Shop, Long> {

    /** Shops that can be scanned: enabled and with at least one list selector. */
    @Query( "select s from Shop s " +
            "where s.disabled = false and (:name is null or s.name = :name) " +
            "and exists (select ls from Selector ls where ls.shop = s and ls.selectorTypeCode = 's_list') " +
            "order by s.name")
    List<Shop> getShops(@Param(value = "name") String name);

    /** Every shop, also disabled ones and ones without a list selector. */
    @Query( "select s from Shop s " +
            "where :name is null or s.name = :name " +
            "order by s.name")
    List<Shop> getAllShops(@Param(value = "name") String name);

    boolean existsByName(String name);

}
