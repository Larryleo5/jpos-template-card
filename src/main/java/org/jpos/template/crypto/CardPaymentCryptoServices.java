package org.jpos.template.crypto;

import java.util.Optional;

import org.jpos.util.Log;
import software.amazon.awssdk.services.paymentcryptographydata.PaymentCryptographyDataClient;

/** 从环境变量解析单例；线程安全懒加载。 */
public final class CardPaymentCryptoServices {
    private static final Log LOG = Log.getLog("Q2", "crypto.card-services");

    private static volatile CardPaymentCryptoService instance;

    private CardPaymentCryptoServices() {}

    public static CardPaymentCryptoService get() {
        if (instance == null) {
            synchronized (CardPaymentCryptoServices.class) {
                if (instance == null) {
                    instance = create();
                }
            }
        }
        return instance;
    }

    static CardPaymentCryptoService create() {
        String pinAlias = trimToNull(System.getenv("APC_PIN_KEY_ALIAS"));
        String macAlias = trimToNull(System.getenv("APC_MAC_KEY_ALIAS"));
        LOG.info("APC alias detection: pin=" + present(pinAlias) + ", mac=" + present(macAlias));
        if (pinAlias == null && macAlias == null) {
            LOG.warn("APC key aliases are not configured, using NoOpCardPaymentCryptoService");
            return NoOpCardPaymentCryptoService.INSTANCE;
        }
        String region = trimToNull(System.getenv("APC_REGION"));
        String regionSource = "APC_REGION";
        if (region == null) {
            region = trimToNull(System.getenv("AWS_REGION"));
            regionSource = "AWS_REGION";
        }
        if (region == null) {
            region = trimToNull(System.getenv("AWS_DEFAULT_REGION"));
            regionSource = "AWS_DEFAULT_REGION";
        }
        if (region == null) {
            region = "ap-southeast-1";
            regionSource = "default";
        }
        String resolvedPinAlias = pinAlias != null ? pinAlias : "";
        String resolvedMacAlias = macAlias != null ? macAlias : resolvedPinAlias;
        LOG.info(
                "Creating APC crypto service, region="
                        + region
                        + " (source="
                        + regionSource
                        + "), pinAlias="
                        + maskAlias(resolvedPinAlias)
                        + ", macAlias="
                        + maskAlias(resolvedMacAlias));
        PaymentCryptographyDataClient client = PaymentCryptographyClientFactory.dataPlane(region);
        return new AwsPaymentCryptographyCardCryptoService(client, resolvedPinAlias, resolvedMacAlias);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String present(String value) {
        return value == null ? "absent" : "present";
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

    /** 测试注入 */
    public static void resetForTests() {
        synchronized (CardPaymentCryptoServices.class) {
            instance = null;
        }
    }

    public static void setForTests(CardPaymentCryptoService svc) {
        synchronized (CardPaymentCryptoServices.class) {
            instance =
                    Optional.ofNullable(svc).orElse(NoOpCardPaymentCryptoService.INSTANCE);
        }
    }
}
