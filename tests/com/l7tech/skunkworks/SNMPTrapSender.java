package com.l7tech.skunkworks;

import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.UdpAddress;

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
public class SNMPTrapSender implements CommandResponder {
    public static final String TRAP_DESTINATION = "192.168.0.3";

    public static void main(String[] args) throws Exception {
        SNMPTrapSender me = new SNMPTrapSender();
        me.sendPDU();
    }

    public SNMPTrapSender() {
        dispatcher = new MessageDispatcherImpl();
        dispatcher.addCommandResponder(this);
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

        // todo, put something useful in this PDU

        dispatcher.sendPdu(new UdpAddress(InetAddress.getByName(TRAP_DESTINATION), 162),
                           SnmpConstants.version2c,
                           SecurityModel.SECURITY_MODEL_SNMPv2c,
                           "ssg".getBytes(), // this should be set to the proper community bytes
                           SecurityLevel.NOAUTH_NOPRIV,
                           pdu,
                           false);
    }

    private MessageDispatcherImpl dispatcher;

    public void processPdu(CommandResponderEvent event) {
        System.out.println("processPdu!"); // should not get here because we are not a listener
    }

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
