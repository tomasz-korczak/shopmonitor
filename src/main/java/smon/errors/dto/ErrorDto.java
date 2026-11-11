package smon.errors.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import smon.model.Shop;

@RequiredArgsConstructor
@Getter
public class ErrorDto {

    private final String sourceClass;
    private final String message;
    private final String reason;
    private final Shop shop;

}
