package org.jpos.template.crypto;

import java.util.Base64;
import java.util.Objects;

import org.jpos.util.Log;
import software.amazon.awssdk.services.paymentcryptographydata.PaymentCryptographyDataClient;
import software.amazon.awssdk.services.paymentcryptographydata.model.GenerateMacRequest;
import software.amazon.awssdk.services.paymentcryptographydata.model.MacAttributes;
import software.amazon.awssdk.services.paymentcryptographydata.model.TranslatePinDataRequest;
import software.amazon.awssdk.services.paymentcryptographydata.model.VerifyMacRequest;

/**
 * APC Data Plane 实现。PIN/MAC 载荷在 API 中为 Base64 字符串；算法须与网络规范一致后再固化到配置。
 */
public class AwsPaymentCryptographyCardCryptoService implements CardPaymentCryptoService {
    private static final Log LOG = Log.getLog("Q2", "crypto.apc-service");

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
        LOG.info(
                "AwsPaymentCryptographyCardCryptoService initialized, pinAlias="
                        + maskAlias(this.defaultPinAlias)
                        + ", macAlias="
                        + maskAlias(this.defaultMacAlias));
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
            LOG.debug("verifyPin requested");
            translatePin(
                    new PinTranslateRequest(
                            request.pinBlock(),
                            request.keyAlias(),
                            request.keyAlias(),
                            java.util.Optional.ofNullable(request.pinBlockFormatHint()),
                            java.util.Optional.empty()));
            return true;
        } catch (CryptoAdapterException e) {
            LOG.warn("verifyPin failed by APC adapter", e);
            return false;
        }
    }

    @Override
    public byte[] translatePin(PinTranslateRequest request) {
        try {
            LOG.debug(
                    "translatePin with incomingAlias="
                            + maskAlias(request.incomingKeyAlias())
                            + ", outgoingAlias="
                            + maskAlias(request.outgoingKeyAlias()));
            TranslatePinDataRequest pinReq =
                    TranslatePinDataRequest.builder()
                            .incomingKeyIdentifier(request.incomingKeyAlias())
                            .outgoingKeyIdentifier(request.outgoingKeyAlias())
                            .encryptedPinBlock(b64(request.incomingPinBlock()))
                            .build();
            String outPin = client.translatePinData(pinReq).pinBlock();
            return fromB64(outPin);
        } catch (Exception e) {
            LOG.warn("translatePin failed", e);
            throw new CryptoAdapterException("translatePin failed", "96", e);
        }
    }

    @Override
    public byte[] generateMac(MacGenerationRequest request) {
        try {
            String algo = request.algorithm() != null && !request.algorithm().isBlank()
                    ? request.algorithm()
                    : DEFAULT_MAC_ALGORITHM;
            LOG.debug(
                    "generateMac with macAlias="
                            + maskAlias(request.macKeyAlias())
                            + ", algorithm="
                            + algo);
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
            LOG.warn("generateMac failed", e);
            throw new CryptoAdapterException("generateMac failed", "96", e);
        }
    }

    @Override
    public boolean verifyMac(MacVerificationRequest request) {
        try {
            String algo = request.algorithm() != null && !request.algorithm().isBlank()
                    ? request.algorithm()
                    : DEFAULT_MAC_ALGORITHM;
            LOG.debug(
                    "verifyMac with macAlias="
                            + maskAlias(request.macKeyAlias())
                            + ", algorithm="
                            + algo);
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
            LOG.warn("verifyMac rejected by APC service", e);
            return false;
        } catch (Exception e) {
            LOG.warn("verifyMac failed", e);
            throw new CryptoAdapterException("verifyMac failed", "96", e);
        }
    }

    @Override
    public String resolveKeyAlias(String logicalKeyId) {
        if (logicalKeyId == null || logicalKeyId.isBlank()) {
            if (!defaultPinAlias.isBlank()) {
                LOG.debug("resolveKeyAlias fallback to default pin alias " + maskAlias(defaultPinAlias));
                return defaultPinAlias;
            }
            LOG.debug("resolveKeyAlias fallback to default mac alias " + maskAlias(defaultMacAlias));
            return defaultMacAlias;
        }
        LOG.debug("resolveKeyAlias using request alias " + maskAlias(logicalKeyId));
        return logicalKeyId;
    }

    private static String maskAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return "<empty>";
        }
        if (alias.length() <= 4) {
            return "****(" + alias.length() + ")";
        }
        return alias.substring(0, 2) + "***" + alias.substring(alias.length() - 2) + "(" + alias.length() + ")";
    }
}
