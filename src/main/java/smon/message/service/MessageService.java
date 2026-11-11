package smon.message.service;


import smon.message.dto.MessageDto;

import java.util.List;

public interface MessageService {
    List<MessageDto> getAllMessages();
}
