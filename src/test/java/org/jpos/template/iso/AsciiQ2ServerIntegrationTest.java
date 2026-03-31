package org.jpos.template.iso;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.XMLPackager;
import org.junit.jupiter.api.Test;

class AsciiQ2ServerIntegrationTest {
    @Test
    void shouldReceiveApprovedResponseFromQ2AsciiServer() throws Exception {
        int port = Integer.getInteger("q2.port", 8080);
        ASCIIChannel channel = new ASCIIChannel("127.0.0.1", port, new XMLPackager());
        channel.connect();
        try {
            ISOMsg request = new ISOMsg("0200");
            request.set(11, "123456");
            request.set(41, "TERMID01");
            request.set(49, "156");

            channel.send(request);
            ISOMsg response = channel.receive();

            assertEquals("0210", response.getMTI());
            assertEquals("00", response.getString(39));
        } finally {
            channel.disconnect();
        }
    }
}
