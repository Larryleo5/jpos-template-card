package org.jpos.template.iso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.template.crypto.NoOpCardPaymentCryptoService;
import org.junit.jupiter.api.Test;

class AsciiEchoRequestListenerTest {

    @Test
    void echoApprovedWithNoOpCrypto() throws Exception {
        AsciiEchoRequestListener listener = new AsciiEchoRequestListener(NoOpCardPaymentCryptoService.INSTANCE);
        ISOMsg req = new ISOMsg("0200");
        req.set(11, "123456");
        req.set(39, "00");

        CapturingSource source = new CapturingSource();
        assertTrue(listener.process(source, req));
        assertEquals("0210", source.sent.getMTI());
        assertEquals("00", source.sent.getString(39));
    }

    static class CapturingSource implements ISOSource {
        ISOMsg sent;

        @Override
        public void send(ISOMsg m) {
            this.sent = m;
        }

        @Override
        public boolean isConnected() {
            return true;
        }
    }
}
