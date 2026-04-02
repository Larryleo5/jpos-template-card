package org.jpos.template.crypto;

import java.util.Optional;

/**
 * AWS Payment Cryptography 业务语义封装；具体 DE 映射由调用方（ISO 监听）决定。
 */
public interface CardPaymentCryptoService {

    /** 是否已配置任一 APC 密钥别名（用于跳过远程调用）。 */
    boolean isApcConfigured();

    boolean verifyPin(PinVerificationRequest request);

    byte[] translatePin(PinTranslateRequest request);

    byte[] generateMac(MacGenerationRequest request);

    boolean verifyMac(MacVerificationRequest request);

    String resolveKeyAlias(String logicalKeyId);

    record PinVerificationRequest(byte[] pinBlock, String pinBlockFormatHint, String keyAlias) {}

    record PinTranslateRequest(
            byte[] incomingPinBlock,
            String incomingKeyAlias,
            String outgoingKeyAlias,
            Optional<String> incomingFormat,
            Optional<String> outgoingFormat) {}

    record MacGenerationRequest(byte[] message, String macKeyAlias, String algorithm) {}

    record MacVerificationRequest(byte[] message, byte[] mac, String macKeyAlias, String algorithm) {}
}
