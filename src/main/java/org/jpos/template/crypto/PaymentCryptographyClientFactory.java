package org.jpos.template.crypto;

import org.jpos.util.Log;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.paymentcryptographydata.PaymentCryptographyDataClient;

/** EKS/IRSA 或本地 ~/.aws/credentials 通过默认凭证链解析。 */
public final class PaymentCryptographyClientFactory {
    private static final Log LOG = Log.getLog("Q2", "crypto.apc-client-factory");

    private PaymentCryptographyClientFactory() {}

    public static PaymentCryptographyDataClient dataPlane(String regionId) {
        LOG.info("Initializing PaymentCryptographyDataClient, region=" + regionId);
        return PaymentCryptographyDataClient.builder().region(Region.of(regionId)).build();
    }
}
