package com.l7tech.skunkworks;

import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.PDU;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;

import java.net.InetAddress;
import java.io.IOException;

/**
 * Test class that uses SNMP4J to send traps.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Nov 30, 2004<br/>
 */
public class SNMPTrapSender {
    public static final String COMMUNITY = "public";
    public static final String TRAP_DESTINATION = "192.168.0.3";
    public static final OID ERROR_MSG_OID = new OID(new int[] {1,3,6,1,7,7,7,7,7,7});

    public static void main(String[] args) throws Exception {
        SNMPTrapSender me = new SNMPTrapSender();
        me.sendPDU();
    }

    public SNMPTrapSender() {
        dispatcher = new MessageDispatcherImpl();
        dispatcher.addMessageProcessingModel(new MPv2c());
        try {
            dispatcher.addTransportMapping(new DefaultUdpTransportMapping());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SecurityProtocols.getInstance().addDefaultProtocols();
    }

    public void sendPDU() throws Exception {
        PDU pdu = new PDU();
        pdu.setType(PDU.TRAP);

        pdu.add(new VariableBinding(ERROR_MSG_OID, new OctetString("blah error something")));

        dispatcher.sendPdu(new UdpAddress(InetAddress.getByName(TRAP_DESTINATION), 162),
                           SnmpConstants.version2c,
                           SecurityModel.SECURITY_MODEL_SNMPv2c,
                           COMMUNITY.getBytes(), // todo, this should be set to the proper community bytes
                           SecurityLevel.NOAUTH_NOPRIV,
                           pdu,
                           false);
    }

    private MessageDispatcherImpl dispatcher;

    /*
     Code to run on remote address to receive this trap (need permission to listen on 162):
     public static void main(String[] args) throws Exception {
        byte[] incomingbuf = new byte[128];
        DatagramPacket incoming = new DatagramPacket(incomingbuf, 128);
        DatagramSocket socket = new DatagramSocket(162);
        socket.receive(incoming);
        System.out.println("Received: " + new String(incomingbuf));
     }
     */
}
