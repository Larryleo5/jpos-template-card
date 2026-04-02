package org.jpos.template.crypto;

import java.util.Optional;

/** 未配置 APC 环境变量时使用；本地测试与 CI 不访问 AWS。 */
public final class NoOpCardPaymentCryptoService implements CardPaymentCryptoService {

    public static final NoOpCardPaymentCryptoService INSTANCE = new NoOpCardPaymentCryptoService();

    private NoOpCardPaymentCryptoService() {}

    @Override
    public boolean isApcConfigured() {
        return false;
    }

    @Override
    public boolean verifyPin(PinVerificationRequest request) {
        return true;
    }

    @Override
    public byte[] translatePin(PinTranslateRequest request) {
        return request.incomingPinBlock().clone();
    }

    @Override
    public byte[] generateMac(MacGenerationRequest request) {
        return new byte[0];
    }

    @Override
    public boolean verifyMac(MacVerificationRequest request) {
        return true;
    }

    @Override
    public String resolveKeyAlias(String logicalKeyId) {
        return logicalKeyId != null && !logicalKeyId.isBlank() ? logicalKeyId : "unset";
    }
}
