package com.l7tech.external.assertions.portaldeployer.server.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.l7tech.external.assertions.portaldeployer.server.ssl.SSLSocketFactoryProvider;
import com.l7tech.external.assertions.portaldeployer.server.ssl.SSLSocketFactoryProviderImpl;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * A Test client to locally test MessageProcessor
 *
 * @author raqri01, 2017-10-24
 */
public class MessageProcessorClientTest {
  public static void main() throws Exception {
    SSLSocketFactoryProvider sfp = new SSLSocketFactoryProviderImpl(null, "");
    sfp.addHost("https://elih4:8443", "7layer");
    MessageProcessor mp = new MessageProcessor(sfp.getSSLSocketFactory());

    ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    Messages testMessages = new Messages();
    Message message1 = new Message();

    message1.setSourceLocation("https://elih4:8443/source");
    message1.setTargetLocation("https://elih4:8443/target");
    message1.setCallbackLocation("https://elih4:8443/callback");
    List<Message> messageList = new ArrayList<>();
    messageList.add(message1);
    testMessages.setMessages(messageList);
    String payload = mapper.writeValueAsString(testMessages);
    MqttMessage message = new MqttMessage(payload.getBytes());
    mp.process(message);
  }
}
