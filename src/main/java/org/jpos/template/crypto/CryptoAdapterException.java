package org.jpos.template.crypto;

/** 映射到 ISO 8583 域 39 等；勿在消息中携带敏感细节。 */
public class CryptoAdapterException extends RuntimeException {

    private final String iso8583ResponseCode;

    public CryptoAdapterException(String message, String iso8583ResponseCode, Throwable cause) {
        super(message, cause);
        this.iso8583ResponseCode = iso8583ResponseCode;
    }

    public String getIso8583ResponseCode() {
        return iso8583ResponseCode;
    }
}
