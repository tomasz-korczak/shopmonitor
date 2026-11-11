package smon.selectors.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import smon.model.Selector;

import java.util.List;

@Repository
public interface SelectorsRepository extends JpaRepository<Selector, Long> {

    @Query("select distinct s.selector from Selector s where selectorTypeCode=:selectorTypeCode")
    public List<String> findAllSelectors(@Param("selectorTypeCode") String selectorTypeCode);
}
