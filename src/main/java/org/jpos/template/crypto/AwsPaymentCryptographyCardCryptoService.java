package org.jpos.template.crypto;

import java.util.Base64;
import java.util.Objects;

import software.amazon.awssdk.services.paymentcryptographydata.PaymentCryptographyDataClient;
import software.amazon.awssdk.services.paymentcryptographydata.model.GenerateMacRequest;
import software.amazon.awssdk.services.paymentcryptographydata.model.MacAttributes;
import software.amazon.awssdk.services.paymentcryptographydata.model.TranslatePinDataRequest;
import software.amazon.awssdk.services.paymentcryptographydata.model.VerifyMacRequest;

/**
 * APC Data Plane 实现。PIN/MAC 载荷在 API 中为 Base64 字符串；算法须与网络规范一致后再固化到配置。
 */
public class AwsPaymentCryptographyCardCryptoService implements CardPaymentCryptoService {

    private final PaymentCryptographyDataClient client;
    private final String defaultPinAlias;
    private final String defaultMacAlias;
    private static final String DEFAULT_MAC_ALGORITHM = "ISO9797_1_ALG3";

    public AwsPaymentCryptographyCardCryptoService(
            PaymentCryptographyDataClient client,
            String defaultPinAlias,
            String defaultMacAlias) {
        this.client = Objects.requireNonNull(client);
        this.defaultPinAlias = defaultPinAlias != null ? defaultPinAlias : "";
        this.defaultMacAlias = defaultMacAlias != null ? defaultMacAlias : "";
    }

    private static String b64(byte[] raw) {
        return Base64.getEncoder().encodeToString(raw);
    }

    private static byte[] fromB64(String s) {
        if (s == null || s.isEmpty()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(s);
    }

    @Override
    public boolean isApcConfigured() {
        return !defaultPinAlias.isBlank() || !defaultMacAlias.isBlank();
    }

    @Override
    public boolean verifyPin(PinVerificationRequest request) {
        try {
            translatePin(
                    new PinTranslateRequest(
                            request.pinBlock(),
                            request.keyAlias(),
                            request.keyAlias(),
                            java.util.Optional.ofNullable(request.pinBlockFormatHint()),
                            java.util.Optional.empty()));
            return true;
        } catch (CryptoAdapterException e) {
            return false;
        }
    }

    @Override
    public byte[] translatePin(PinTranslateRequest request) {
        try {
            TranslatePinDataRequest pinReq =
                    TranslatePinDataRequest.builder()
                            .incomingKeyIdentifier(request.incomingKeyAlias())
                            .outgoingKeyIdentifier(request.outgoingKeyAlias())
                            .encryptedPinBlock(b64(request.incomingPinBlock()))
                            .build();
            String outPin = client.translatePinData(pinReq).pinBlock();
            return fromB64(outPin);
        } catch (Exception e) {
            throw new CryptoAdapterException("translatePin failed", "96", e);
        }
    }

    @Override
    public byte[] generateMac(MacGenerationRequest request) {
        try {
            String algo = request.algorithm() != null && !request.algorithm().isBlank()
                    ? request.algorithm()
                    : DEFAULT_MAC_ALGORITHM;
            String macStr =
                    client.generateMac(
                                    GenerateMacRequest.builder()
                                            .keyIdentifier(request.macKeyAlias())
                                            .messageData(b64(request.message()))
                                            .generationAttributes(
                                                    MacAttributes.builder().algorithm(algo).build())
                                            .build())
                            .mac();
            return fromB64(macStr);
        } catch (Exception e) {
            throw new CryptoAdapterException("generateMac failed", "96", e);
        }
    }

    @Override
    public boolean verifyMac(MacVerificationRequest request) {
        try {
            String algo = request.algorithm() != null && !request.algorithm().isBlank()
                    ? request.algorithm()
                    : DEFAULT_MAC_ALGORITHM;
            client.verifyMac(
                    VerifyMacRequest.builder()
                            .keyIdentifier(request.macKeyAlias())
                            .messageData(b64(request.message()))
                            .mac(b64(request.mac()))
                            .verificationAttributes(
                                    MacAttributes.builder().algorithm(algo).build())
                            .build());
            return true;
        } catch (software.amazon.awssdk.awscore.exception.AwsServiceException e) {
            return false;
        } catch (Exception e) {
            throw new CryptoAdapterException("verifyMac failed", "96", e);
        }
    }

    @Override
    public String resolveKeyAlias(String logicalKeyId) {
        if (logicalKeyId == null || logicalKeyId.isBlank()) {
            if (!defaultPinAlias.isBlank()) {
                return defaultPinAlias;
            }
            return defaultMacAlias;
        }
        return logicalKeyId;
    }
}
