package smon.message.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smon.message.dto.MessageDto;
import smon.message.dao.MessageRepository;
import smon.model.Message;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService{

    @Autowired
    private MessageRepository messageRepository;

    @Override
    public List<MessageDto> getAllMessages() {
        return messageRepository.findAll().stream().map(MessageServiceImpl::mapToDto).collect(Collectors.toList());
    }

    private static MessageDto mapToDto(Message message) {
        return new MessageDto(message.getMessage(), message.getType());
    }
}
