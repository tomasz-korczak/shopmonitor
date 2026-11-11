package smon.errors.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import smon.model.ErrorRecord;

import java.time.LocalDate;
import java.util.List;

public interface ErrorsRepository extends JpaRepository<ErrorRecord, Long> {

    List<ErrorRecord> findAllByRegistrationDateGreaterThanEqualOrderByIdAsc(LocalDate since);

}
