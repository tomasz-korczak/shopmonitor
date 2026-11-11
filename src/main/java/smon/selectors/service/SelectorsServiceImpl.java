package smon.selectors.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smon.model.Selector;
import smon.selectors.dao.SelectorsRepository;

import java.util.Collection;
import java.util.List;

@Service
public class SelectorsServiceImpl implements SelectorsService{

    @Autowired
    private SelectorsRepository selectorsRepository;

    @Override
    public List<String> getAllSelectors(String code) {
        return selectorsRepository.findAllSelectors(code);
    }

    @Override
    @Transactional
    public void saveSelectors(Collection<Selector> selectors) {
        selectorsRepository.saveAll(selectors);
    }
}
