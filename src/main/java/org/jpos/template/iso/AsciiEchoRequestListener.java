package org.jpos.template.iso;

import java.io.IOException;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

public class AsciiEchoRequestListener implements ISORequestListener {
    @Override
    public boolean process(ISOSource source, ISOMsg request) {
        try {
            ISOMsg response = (ISOMsg) request.clone();
            response.setResponseMTI();
            response.set(39, "00");
            source.send(response);
            return true;
        } catch (ISOException | IOException e) {
            return false;
        }
    }
}
