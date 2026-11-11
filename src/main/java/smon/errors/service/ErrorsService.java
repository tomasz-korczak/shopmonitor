package smon.errors.service;

import smon.model.Shop;

public interface ErrorsService {
    void reportError(String message, Shop shop, Throwable originalException);
    void reportError(String message, Shop shop, String reason);
}
