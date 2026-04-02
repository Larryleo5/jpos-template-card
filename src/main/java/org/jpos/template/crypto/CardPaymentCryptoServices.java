package org.jpos.template.crypto;

import java.util.Optional;

import software.amazon.awssdk.services.paymentcryptographydata.PaymentCryptographyDataClient;

/** 从环境变量解析单例；线程安全懒加载。 */
public final class CardPaymentCryptoServices {

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
        if (pinAlias == null && macAlias == null) {
            return NoOpCardPaymentCryptoService.INSTANCE;
        }
        String region =
                trimToNull(System.getenv("APC_REGION"));
        if (region == null) {
            region = trimToNull(System.getenv("AWS_REGION"));
        }
        if (region == null) {
            region = trimToNull(System.getenv("AWS_DEFAULT_REGION"));
        }
        if (region == null) {
            region = "ap-southeast-1";
        }
        PaymentCryptographyDataClient client = PaymentCryptographyClientFactory.dataPlane(region);
        String pin = pinAlias != null ? pinAlias : "";
        String mac = macAlias != null ? macAlias : pin;
        return new AwsPaymentCryptographyCardCryptoService(client, pin, mac);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
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
