package smon.message.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smon.model.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
}
