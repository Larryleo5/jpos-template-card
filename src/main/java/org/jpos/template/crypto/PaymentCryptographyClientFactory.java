package org.jpos.template.crypto;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.paymentcryptographydata.PaymentCryptographyDataClient;

/** EKS/IRSA 或本地 ~/.aws/credentials 通过默认凭证链解析。 */
public final class PaymentCryptographyClientFactory {

    private PaymentCryptographyClientFactory() {}

    public static PaymentCryptographyDataClient dataPlane(String regionId) {
        return PaymentCryptographyDataClient.builder().region(Region.of(regionId)).build();
    }
}
