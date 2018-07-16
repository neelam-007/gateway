package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.common.io.XmlUtil;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 4/1/13
 * Time: 12:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class HL7CodecTest {

    private static final String CHARSET = "ISO-8859-1";
    private static final byte START_BYTE = 0x0b;
    private static final byte END_BYTE1 = 0x1c;
    private static final byte END_BYTE2 = 0x0d;

    private static final String HL7_MESSAGE =
            "MSH|^~\\&|MESA_RPT_MGR|EAST_RADIOLOGY|REPOSITORY|XYZ|||ORU^R01|MESA3b781ae8|P|2.5||||||||\r" +
                    "PID|||CR3^^^ADT1||CRTHREE^PAUL|||||||||||||PatientAcct||||||||||||\r" +
                    "OBR||||4550|||20010501141500.0000||||||||||||||||||F||||||||||||||||||\r" +
                    "OBX|1|HD|SR Instance UID||1.113654.1.2001.30.2.1||||||F||||||\r" +
                    "OBX|2|TX|SR Text||Lungs expanded and clear. Conclusions Normal PA chest x-ray.||||||F||||||\r" +
                    "OBR||||4551|||20010501141500||||||||||||||||||F||||||||||||||||||\r" +
                    "OBX|1|HD|SR Instance UID||1.113654.1.2001.10.2.1.603||||||F||||||\r" +
                    "OBX|2|HD|Study Instance UID|1|1.113654.1.2001.10||||||F||||||\r" +
                    "OBX|3|HD|Series Instance UID|1|1.113654.1.2001.10.1||||||F||||||";

    private static final String HL7_MESSAGE_WITH_START_END_BYTES =
            String.valueOf(Character.toChars(START_BYTE)) +
                    HL7_MESSAGE +
                    String.valueOf(Character.toChars(END_BYTE1)) +
                    String.valueOf(Character.toChars(END_BYTE2));

    private static final String HL7_XML =
            "<HL7><MSH><MSH.1>|</MSH.1><MSH.2>^~\\&amp;</MSH.2><MSH.3>MESA_RPT_MGR</MSH.3><MSH.4>EAST_RADIOLOGY</MSH.4><MSH.5>REPOSITORY</MSH.5><MSH.6>XYZ</MSH.6><MSH.7/><MSH.8/><MSH.9><MSH.9.1>ORU</MSH.9.1><MSH.9.2>R01</MSH.9.2></MSH.9><MSH.10>MESA3b781ae8</MSH.10><MSH.11>P</MSH.11><MSH.12>2.5</MSH.12><MSH.13/><MSH.14/><MSH.15/><MSH.16/><MSH.17/><MSH.18/><MSH.19/><MSH.20/></MSH><PID><PID.1/><PID.2/><PID.3><PID.3.1>CR3</PID.3.1><PID.3.2/><PID.3.3/><PID.3.4>ADT1</PID.3.4></PID.3><PID.4/><PID.5><PID.5.1>CRTHREE</PID.5.1><PID.5.2>PAUL</PID.5.2></PID.5><PID.6/><PID.7/><PID.8/><PID.9/><PID.10/><PID.11/><PID.12/><PID.13/><PID.14/><PID.15/><PID.16/><PID.17/><PID.18>PatientAcct</PID.18><PID.19/><PID.20/><PID.21/><PID.22/><PID.23/><PID.24/><PID.25/><PID.26/><PID.27/><PID.28/><PID.29/><PID.30/></PID><OBR><OBR.1/><OBR.2/><OBR.3/><OBR.4>4550</OBR.4><OBR.5/><OBR.6/><OBR.7>20010501141500.0000</OBR.7><OBR.8/><OBR.9/><OBR.10/><OBR.11/><OBR.12/><OBR.13/><OBR.14/><OBR.15/><OBR.16/><OBR.17/><OBR.18/><OBR.19/><OBR.20/><OBR.21/><OBR.22/><OBR.23/><OBR.24/><OBR.25>F</OBR.25><OBR.26/><OBR.27/><OBR.28/><OBR.29/><OBR.30/><OBR.31/><OBR.32/><OBR.33/><OBR.34/><OBR.35/><OBR.36/><OBR.37/><OBR.38/><OBR.39/><OBR.40/><OBR.41/><OBR.42/><OBR.43/></OBR><OBX><OBX.1>1</OBX.1><OBX.2>HD</OBX.2><OBX.3>SR Instance UID</OBX.3><OBX.4/><OBX.5>1.113654.1.2001.30.2.1</OBX.5><OBX.6/><OBX.7/><OBX.8/><OBX.9/><OBX.10/><OBX.11>F</OBX.11><OBX.12/><OBX.13/><OBX.14/><OBX.15/><OBX.16/><OBX.17/></OBX><OBX><OBX.1>2</OBX.1><OBX.2>TX</OBX.2><OBX.3>SR Text</OBX.3><OBX.4/><OBX.5>Lungs expanded and clear. Conclusions Normal PA chest x-ray.</OBX.5><OBX.6/><OBX.7/><OBX.8/><OBX.9/><OBX.10/><OBX.11>F</OBX.11><OBX.12/><OBX.13/><OBX.14/><OBX.15/><OBX.16/><OBX.17/></OBX><OBR><OBR.1/><OBR.2/><OBR.3/><OBR.4>4551</OBR.4><OBR.5/><OBR.6/><OBR.7>20010501141500</OBR.7><OBR.8/><OBR.9/><OBR.10/><OBR.11/><OBR.12/><OBR.13/><OBR.14/><OBR.15/><OBR.16/><OBR.17/><OBR.18/><OBR.19/><OBR.20/><OBR.21/><OBR.22/><OBR.23/><OBR.24/><OBR.25>F</OBR.25><OBR.26/><OBR.27/><OBR.28/><OBR.29/><OBR.30/><OBR.31/><OBR.32/><OBR.33/><OBR.34/><OBR.35/><OBR.36/><OBR.37/><OBR.38/><OBR.39/><OBR.40/><OBR.41/><OBR.42/><OBR.43/></OBR><OBX><OBX.1>1</OBX.1><OBX.2>HD</OBX.2><OBX.3>SR Instance UID</OBX.3><OBX.4/><OBX.5>1.113654.1.2001.10.2.1.603</OBX.5><OBX.6/><OBX.7/><OBX.8/><OBX.9/><OBX.10/><OBX.11>F</OBX.11><OBX.12/><OBX.13/><OBX.14/><OBX.15/><OBX.16/><OBX.17/></OBX><OBX><OBX.1>2</OBX.1><OBX.2>HD</OBX.2><OBX.3>Study Instance UID</OBX.3><OBX.4>1</OBX.4><OBX.5>1.113654.1.2001.10</OBX.5><OBX.6/><OBX.7/><OBX.8/><OBX.9/><OBX.10/><OBX.11>F</OBX.11><OBX.12/><OBX.13/><OBX.14/><OBX.15/><OBX.16/><OBX.17/></OBX><OBX><OBX.1>3</OBX.1><OBX.2>HD</OBX.2><OBX.3>Series Instance UID</OBX.3><OBX.4>1</OBX.4><OBX.5>1.113654.1.2001.10.1</OBX.5><OBX.6/><OBX.7/><OBX.8/><OBX.9/><OBX.10/><OBX.11>F</OBX.11><OBX.12/><OBX.13/><OBX.14/><OBX.15/><OBX.16/><OBX.17/></OBX></HL7>";

    @Test
    public void testEncode() throws Exception {
        HL7Codec codec = new HL7Codec();
        codec.setCharset(CHARSET);
        codec.setStartByte(START_BYTE);
        codec.setEndByte1(END_BYTE1);
        codec.setEndByte2(END_BYTE2);

        EncoderOut eOut = new EncoderOut();
        codec.getEncoder(null).encode(null, HL7_XML, eOut);
        byte[] actual = eOut.getOutput();

        Assert.assertArrayEquals(HL7_MESSAGE_WITH_START_END_BYTES.getBytes(CHARSET), actual);
    }


    @Test
    public void testDecode() throws Exception {
        HL7Codec codec = new HL7Codec();
        codec.setCharset(CHARSET);
        codec.setStartByte(START_BYTE);
        codec.setEndByte1(END_BYTE1);
        codec.setEndByte2(END_BYTE2);

        IoBuffer ioBuffer = null;
        ioBuffer = IoBuffer.allocate(HL7_MESSAGE_WITH_START_END_BYTES.getBytes(CHARSET).length);
        ioBuffer.put(HL7_MESSAGE_WITH_START_END_BYTES.getBytes(CHARSET));
        ioBuffer.flip();

        IoSession ioSession = mock(IoSession.class);
        DecoderOut dOut = new DecoderOut();
        codec.getDecoder(null).decode(ioSession, ioBuffer, dOut);
        String actual = dOut.getOutput();

        Assert.assertEquals(HL7_XML, actual);
    }

    @Test
    public void testMarshaler() throws Exception {
        HL7Marshaler unmarshaler = new HL7Marshaler();
        byte[] xml = unmarshaler.unmarshal(HL7_MESSAGE.getBytes());
        Assert.assertTrue(XmlUtil.isValidXmlContent(new String(xml, CHARSET)));

        HL7Marshaler marshaler = new HL7Marshaler();
        byte[] hl7 = marshaler.marshal(xml);
        Assert.assertEquals(HL7_MESSAGE, new String(hl7, CHARSET));
    }
}