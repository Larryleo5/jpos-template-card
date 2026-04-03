package org.jpos.template.iso;

import java.io.IOException;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.template.crypto.CardPaymentCryptoService;
import org.jpos.template.crypto.CardPaymentCryptoServices;
import org.jpos.util.Log;

/**
 * Demo 监听：Echo + 可选 APC 挂钩（入站 DE52 PIN / DE64 MAC）。未配置 {@code APC_*_KEY_ALIAS} 时与纯 Echo 行为一致。
 */
public class AsciiEchoRequestListener implements ISORequestListener {
    private static final Log LOG = Log.getLog("Q2", "iso.ascii-echo-listener");

    private final CardPaymentCryptoService crypto;

    public AsciiEchoRequestListener() {
        this(CardPaymentCryptoServices.get());
    }

    AsciiEchoRequestListener(CardPaymentCryptoService crypto) {
        this.crypto = crypto;
    }

    @Override
    public boolean process(ISOSource source, ISOMsg request) {
        try {
            String pinAlias = trimEnv("APC_PIN_KEY_ALIAS");
            String macAlias = trimEnv("APC_MAC_KEY_ALIAS");
            boolean apcConfigured = crypto.isApcConfigured();

            LOG.info(
                    "Received ISO request: mti="
                            + valueOrDash(request.getMTI())
                            + ", de3="
                            + valueOrDash(request.getString(3))
                            + ", de4="
                            + valueOrDash(request.getString(4))
                            + ", de11="
                            + valueOrDash(request.getString(11))
                            + ", de41="
                            + valueOrDash(request.getString(41))
                            + ", has52="
                            + request.hasField(52)
                            + ", has64="
                            + request.hasField(64));
            LOG.info(
                    "APC key status: env.pin="
                            + present(pinAlias)
                            + ", env.mac="
                            + present(macAlias)
                            + ", service.isApcConfigured="
                            + apcConfigured);

            if (pinAlias != null && request.hasField(52)) {
                byte[] pinBlock = request.getBytes(52);
                if (!crypto.verifyPin(
                        new CardPaymentCryptoService.PinVerificationRequest(
                                pinBlock, "ISO-0", crypto.resolveKeyAlias(pinAlias)))) {
                    return sendDecline(source, request, "55");
                }
            }

            if (macAlias != null && request.hasField(64)) {
                byte[] mac = request.getBytes(64);
                byte[] macPayload = macCanonicalPayload(request);
                if (!crypto.verifyMac(
                        new CardPaymentCryptoService.MacVerificationRequest(
                                macPayload, mac, crypto.resolveKeyAlias(macAlias), null))) {
                    return sendDecline(source, request, "96");
                }
            }

            ISOMsg response = (ISOMsg) request.clone();
            response.setResponseMTI();
            response.set(39, "00");

            if (macAlias != null && apcConfigured) {
                try {
                    byte[] macPayload = macCanonicalPayload(response);
                    byte[] macOut =
                            crypto.generateMac(
                                    new CardPaymentCryptoService.MacGenerationRequest(
                                            macPayload, crypto.resolveKeyAlias(macAlias), null));
                    if (macOut.length > 0) {
                        response.set(64, macOut);
                    }
                } catch (Exception ignored) {
                    // Demo：响应 MAC 失败时不阻塞 Echo，避免无 KMS 环境测试失败
                }
            }

            source.send(response);
            return true;
        } catch (ISOException | IOException e) {
            return false;
        }
    }

    /** Demo：用于 MAC 的规范域拼接（生产需按网络规范替换）。 */
    private static byte[] macCanonicalPayload(ISOMsg m) throws ISOException {
        StringBuilder sb = new StringBuilder();
        sb.append(m.getMTI());
        if (m.hasField(2)) {
            sb.append(m.getString(2));
        }
        if (m.hasField(4)) {
            sb.append(m.getString(4));
        }
        if (m.hasField(11)) {
            sb.append(m.getString(11));
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static boolean sendDecline(ISOSource source, ISOMsg request, String de39)
            throws ISOException, IOException {
        ISOMsg response = (ISOMsg) request.clone();
        response.setResponseMTI();
        response.set(39, de39);
        source.send(response);
        return true;
    }

    private static String trimEnv(String name) {
        String v = System.getenv(name);
        if (v == null) {
            return null;
        }
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String present(String value) {
        return value == null ? "absent" : "present";
    }
}
