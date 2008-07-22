package com.l7tech.skunkworks;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.Serializable;
import java.net.URL;
import javax.naming.spi.InitialContextFactory;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.Binding;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueConnection;
import javax.jms.JMSException;
import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.Destination;
import javax.jms.ServerSessionPool;
import javax.jms.Topic;
import javax.jms.Session;
import javax.jms.ExceptionListener;
import javax.jms.ConnectionMetaData;
import javax.jms.QueueSession;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.TemporaryQueue;
import javax.jms.BytesMessage;
import javax.jms.MessageConsumer;
import javax.jms.TopicSubscriber;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.MessageProducer;
import javax.jms.StreamMessage;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.MessageListener;

/**
 * JMS Stub for JMS Routing testing (JMS Echo).
 *
 * To build the JMS Stub:
 * ./build.sh compile-tests
 * pushd build/test-classes; jar cf ../../StubJMS.jar $(ls com/l7tech/skunkworks/StubJMSInitialContextFactory*); popd
 * cp StubJMS.jar /ssg/lib/ext
 *
 * To configure the JMS Stub:
 * Direction: Outbound
 * JNDI Connection URL: http://localhost/?sendFailureFrequency=5&receiveFailureFrequency=10
 * Initial Context Factory Class: com.l7tech.skunkworks.StubJMSInitialContextFactory
 * Queue Connection Factory Name: QueueConnectionFactory
 * Queue Name: *
 */
public class StubJMSInitialContextFactory implements InitialContextFactory {

    private static final Logger logger = Logger.getLogger(StubJMSInitialContextFactory.class.getName());

    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return new StubJMSContext(environment);
    }

    public static final class StubJMSContext implements Context {

        private Hashtable environment;

        public StubJMSContext(Hashtable environment) {
            this.environment = environment;
        }

        @SuppressWarnings( { "unchecked" } )
        private void populateEnv() {
            String urlStr = (String) environment.get(Context.PROVIDER_URL);
            if ( urlStr != null ) {
                try {
                    URL url = new URL(urlStr);
                    String qs = url.getQuery();
                    if ( qs != null ) {
                        StringTokenizer strtok = new StringTokenizer(qs, "&");
                        while( strtok.hasMoreTokens() ) {
                            String token = strtok.nextToken();
                            if ( token != null ) {
                                int index = token.indexOf( '=' );
                                if ( index > 0 ) {
                                    String name = token.substring( 0, index );
                                    String value = token.substring( index+1 );
                                    System.out.println("StubJMS property " + name + " = " + value);
                                    environment.put( name, value );
                                } else {
                                    environment.put( token, "" );
                                }
                            }
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public Object addToEnvironment(String propName, Object propVal) throws NamingException {
            return null;
        }

        public void bind(Name name, Object obj) throws NamingException {
        }

        public void bind(String name, Object obj) throws NamingException {
        }

        public void close() throws NamingException {
        }

        public Name composeName(Name name, Name prefix) throws NamingException {
            return null;
        }

        public String composeName(String name, String prefix) throws NamingException {
            return null;
        }

        public Context createSubcontext(Name name) throws NamingException {
            return null;
        }

        public Context createSubcontext(String name) throws NamingException {
            return null;
        }

        public void destroySubcontext(Name name) throws NamingException {
        }

        public void destroySubcontext(String name) throws NamingException {
        }

        public Hashtable<?, ?> getEnvironment() throws NamingException {
            return environment;
        }

        public String getNameInNamespace() throws NamingException {
            return null;
        }

        public NameParser getNameParser(Name name) throws NamingException {
            return null;
        }

        public NameParser getNameParser(String name) throws NamingException {
            return null;
        }

        public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
            return null;
        }

        public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
            return null;
        }

        public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
            return null;
        }

        public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
            return null;
        }

        public Object lookup(Name name) throws NamingException {
            logger.log(Level.INFO, "Performing lookup for '"+ Arrays.asList(name.getAll())+"'.");
            return lookup(name.get(name.size()-1));
        }

        public Object lookup(String name) throws NamingException {
            logger.log(Level.INFO, "Performing lookup for '"+name+"'.");

            if (name.toLowerCase().endsWith("connectionfactory")) {
                populateEnv();
                return new StubJMSQueueConnectionFactory(environment);
            }
            else {
                return new StubJMSQueueDestination();    
            }
        }

        public Object lookupLink(Name name) throws NamingException {
            return null;
        }

        public Object lookupLink(String name) throws NamingException {
            return null;
        }

        public void rebind(Name name, Object obj) throws NamingException {
        }

        public void rebind(String name, Object obj) throws NamingException {
        }

        public Object removeFromEnvironment(String propName) throws NamingException {
            return null;
        }

        public void rename(Name oldName, Name newName) throws NamingException {
        }

        public void rename(String oldName, String newName) throws NamingException {
        }

        public void unbind(Name name) throws NamingException {
        }

        public void unbind(String name) throws NamingException {
        }
    }

    public static final class StubJMSQueueConnectionFactory implements QueueConnectionFactory {
        
        private Hashtable<?, ?> environment;

        private StubJMSQueueConnectionFactory( Hashtable<?, ?> environment ) {
            this.environment = environment;
        }

        public QueueConnection createQueueConnection() throws JMSException {
            logger.info("Creating QueueConnection");
            return new StubJMSQueueConnection(environment);
        }

        public QueueConnection createQueueConnection(String string, String string1) throws JMSException {
            logger.info("Creating QueueConnection");
            return new StubJMSQueueConnection(environment);
        }

        public Connection createConnection() throws JMSException {
            logger.info("Creating QueueConnection");
            return new StubJMSQueueConnection(environment);
        }

        public Connection createConnection(String string, String string1) throws JMSException {
            logger.info("Creating QueueConnection");
            return new StubJMSQueueConnection(environment);
        }
    }

    public static final class StubJMSQueueConnection implements QueueConnection {

        private Hashtable<?, ?> environment;

        private StubJMSQueueConnection( Hashtable<?, ?> environment ) {
            this.environment = environment;
        }

        public void close() throws JMSException {
        }

        public ConnectionConsumer createConnectionConsumer(Destination destination, String string, ServerSessionPool serverSessionPool, int i) throws JMSException {
            return null;
        }

        public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String string, String string1, ServerSessionPool serverSessionPool, int i) throws JMSException {
            return null;
        }

        public Session createSession(boolean b, int i) throws JMSException {
            logger.info("Creating Session");
            return new StubJMSQueueSession(environment);
        }

        public String getClientID() throws JMSException {
            return null;
        }

        public ExceptionListener getExceptionListener() throws JMSException {
            return null;
        }

        public ConnectionMetaData getMetaData() throws JMSException {
            return null;
        }

        public void setClientID(String string) throws JMSException {
        }

        public void setExceptionListener(ExceptionListener exceptionListener) throws JMSException {
        }

        public void start() throws JMSException {
        }

        public void stop() throws JMSException {
        }

        public QueueSession createQueueSession(boolean b, int i) throws JMSException {
            logger.info("Creating QueueSession");
            return new StubJMSQueueSession(environment);
        }

        public ConnectionConsumer createConnectionConsumer(Queue queue, String string, ServerSessionPool serverSessionPool, int i) throws JMSException {
            return null;
        }
    }

    public static class StubJMSQueueSession implements QueueSession {
        private int sendCount = 0;
        private int receiveCount = 0;

        private Message message;

        private Hashtable environment;

        private StubJMSQueueSession( Hashtable environment ) {
            this.environment = environment;
        }

        boolean doSendFailure() {
            boolean fail = false;

            try {
                String value = (String) environment.get( "sendFailureFrequency" );
                if ( value != null ) {
                    int failures = Integer.parseInt( value );
                    System.out.println( "JMSStub using send failure frequency: " + failures );

                    if ( failures > 0 && ++sendCount%failures==0 ) {
                        fail = true;
                    }                    
                }
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }

            return fail;
        }

        boolean doReceiveFailure() {
            boolean fail = false;

            try {
                String value = (String) environment.get( "receiveFailureFrequency" );
                if ( value != null ) {
                    int failures = Integer.parseInt( value );
                    System.out.println( "JMSStub using receive failure frequency: " + failures );

                    if ( failures > 0 && ++receiveCount%failures==0 ) {
                        fail = true;
                    }
                }
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }

            return fail;
        }

        Message getMessage() {
            return message;
        }

        void setMessage(Message message) {
            this.message = message;
        }

        public QueueBrowser createBrowser(Queue queue) throws JMSException {
            return null;
        }

        public QueueBrowser createBrowser(Queue queue, String string) throws JMSException {
            return null;
        }

        public Queue createQueue(String string) throws JMSException {
            return null;
        }

        public QueueReceiver createReceiver(Queue queue) throws JMSException {
            return new StubJMSQueueReceiver(this);
        }

        public QueueReceiver createReceiver(Queue queue, String string) throws JMSException {
            return new StubJMSQueueReceiver(this);
        }

        public QueueSender createSender(Queue queue) throws JMSException {
            return new StubJMSQueueSender(this);
        }

        public TemporaryQueue createTemporaryQueue() throws JMSException {
            return new StubJMSQueueDestination();
        }

        public void close() throws JMSException {
        }

        public void commit() throws JMSException {
        }

        public BytesMessage createBytesMessage() throws JMSException {
            return new StubJMSMessage();
        }

        public MessageConsumer createConsumer(Destination destination) throws JMSException {
            return null;
        }

        public MessageConsumer createConsumer(Destination destination, String string) throws JMSException {
            return null;
        }

        public MessageConsumer createConsumer(Destination destination, String string, boolean b) throws JMSException {
            return null;
        }

        public TopicSubscriber createDurableSubscriber(Topic topic, String string) throws JMSException {
            return null;
        }

        public TopicSubscriber createDurableSubscriber(Topic topic, String string, String string1, boolean b) throws JMSException {
            return null;
        }

        public MapMessage createMapMessage() throws JMSException {
            return null;
        }

        public Message createMessage() throws JMSException {
            return null;
        }

        public ObjectMessage createObjectMessage() throws JMSException {
            return null;
        }

        public ObjectMessage createObjectMessage(Serializable serializable) throws JMSException {
            return null;
        }

        public MessageProducer createProducer(Destination destination) throws JMSException {
            return null;
        }

        public StreamMessage createStreamMessage() throws JMSException {
            return null;
        }

        public TemporaryTopic createTemporaryTopic() throws JMSException {
            return null;
        }

        public TextMessage createTextMessage() throws JMSException {
            return new StubJMSMessage();
        }

        public TextMessage createTextMessage(String string) throws JMSException {
            TextMessage message = new StubJMSMessage();
            message.setText(string);
            return message;
        }

        public Topic createTopic(String string) throws JMSException {
            return null;
        }

        public int getAcknowledgeMode() throws JMSException {
            return 0;
        }

        public MessageListener getMessageListener() throws JMSException {
            return null;
        }

        public boolean getTransacted() throws JMSException {
            return false;
        }

        public void recover() throws JMSException {
        }

        public void rollback() throws JMSException {
        }

        public void run() {
        }

        public void setMessageListener(MessageListener messageListener) throws JMSException {
        }

        public void unsubscribe(String string) throws JMSException {
        }
    }

    public static class StubJMSQueueDestination implements TemporaryQueue {
        
        public void delete() throws JMSException {
        }

        public String getQueueName() throws JMSException {
            return null;
        }
    }

    public static class StubJMSQueueReceiver implements QueueReceiver {

        private StubJMSQueueSession stubSession;

        public StubJMSQueueReceiver(StubJMSQueueSession session) {
            this.stubSession = session;
        }

        private void checkFail() throws JMSException {
            if ( stubSession.doReceiveFailure() ) {
                throw new JMSException("Simulated JMS receive failure");
            }
        }

        public Queue getQueue() throws JMSException {
            return null;
        }

        public void close() throws JMSException {
        }

        public MessageListener getMessageListener() throws JMSException {
            return null;
        }

        public String getMessageSelector() throws JMSException {
            return null;
        }

        public Message receive() throws JMSException {
            checkFail();
            return stubSession.getMessage();
        }

        public Message receive(long l) throws JMSException {
            checkFail();
            return stubSession.getMessage();
        }

        public Message receiveNoWait() throws JMSException {
            checkFail();
            return stubSession.getMessage();
        }

        public void setMessageListener(MessageListener messageListener) throws JMSException {
        }
    }

    public static class StubJMSQueueSender implements QueueSender {
        private StubJMSQueueSession stubSession;

        public StubJMSQueueSender(StubJMSQueueSession session) {
            stubSession = session;
        }

        private void checkFail() throws JMSException {
            if ( stubSession.doSendFailure() ) {
                throw new JMSException("Simulated JMS send failure");
            }
        }

        public Queue getQueue() throws JMSException {
            return null;
        }

        public void send(Message message) throws JMSException {
            checkFail();
            stubSession.setMessage(message);
        }

        public void send(Message message, int i, int i1, long l) throws JMSException {
            checkFail();
            stubSession.setMessage(message);
        }

        public void send(Queue queue, Message message) throws JMSException {
            checkFail();
            stubSession.setMessage(message);
        }

        public void send(Queue queue, Message message, int i, int i1, long l) throws JMSException {
            checkFail();
            stubSession.setMessage(message);
        }

        public void close() throws JMSException {
        }

        public int getDeliveryMode() throws JMSException {
            return 0;
        }

        public Destination getDestination() throws JMSException {
            return null;
        }

        public boolean getDisableMessageID() throws JMSException {
            return false;
        }

        public boolean getDisableMessageTimestamp() throws JMSException {
            return false;
        }

        public int getPriority() throws JMSException {
            return 0;
        }

        public long getTimeToLive() throws JMSException {
            return 0;
        }

        public void send(Destination destination, Message message) throws JMSException {
            stubSession.setMessage(message);
        }

        public void send(Destination destination, Message message, int i, int i1, long l) throws JMSException {
            stubSession.setMessage(message);
        }

        public void setDeliveryMode(int i) throws JMSException {
        }

        public void setDisableMessageID(boolean b) throws JMSException {
        }

        public void setDisableMessageTimestamp(boolean b) throws JMSException {
        }

        public void setPriority(int i) throws JMSException {
        }

        public void setTimeToLive(long l) throws JMSException {
        }
    }

    public static class StubJMSMessage implements BytesMessage, TextMessage {
        private String messageText = "";
        private Destination replyTo;
        private String correlationID = "";
        private Map<String,Object> properties = new HashMap<String,Object>();

        public long getBodyLength() throws JMSException {
            return messageText.length();
        }

        public boolean readBoolean() throws JMSException {
            return false;
        }

        public byte readByte() throws JMSException {
            return 0;
        }

        public int readBytes(byte[] bytes) throws JMSException {
            return 0;
        }

        public int readBytes(byte[] bytes, int i) throws JMSException {
            return 0;
        }

        public char readChar() throws JMSException {
            return 0;
        }

        public double readDouble() throws JMSException {
            return 0;
        }

        public float readFloat() throws JMSException {
            return 0;
        }

        public int readInt() throws JMSException {
            return 0;
        }

        public long readLong() throws JMSException {
            return 0;
        }

        public short readShort() throws JMSException {
            return 0;
        }

        public int readUnsignedByte() throws JMSException {
            return 0;
        }

        public int readUnsignedShort() throws JMSException {
            return 0;
        }

        public String readUTF() throws JMSException {
            return null;
        }

        public void reset() throws JMSException {
        }

        public void writeBoolean(boolean b) throws JMSException {
        }

        public void writeByte(byte b) throws JMSException {
        }

        public void writeBytes(byte[] bytes) throws JMSException {
            messageText = new String(bytes);
        }

        public void writeBytes(byte[] bytes, int i, int i1) throws JMSException {
        }

        public void writeChar(char c) throws JMSException {
        }

        public void writeDouble(double v) throws JMSException {
        }

        public void writeFloat(float v) throws JMSException {
        }

        public void writeInt(int i) throws JMSException {
        }

        public void writeLong(long l) throws JMSException {
        }

        public void writeObject(Object object) throws JMSException {
        }

        public void writeShort(short i) throws JMSException {
        }

        public void writeUTF(String string) throws JMSException {
        }

        public void acknowledge() throws JMSException {
        }

        public void clearBody() throws JMSException {
        }

        public void clearProperties() throws JMSException {
            properties.clear();
        }

        public boolean getBooleanProperty(String string) throws JMSException {
            logger.info("Boolean property get '" + string + "'");
            return (Boolean) properties.get(string);
        }

        public byte getByteProperty(String string) throws JMSException {
            logger.info("Byte property get '" + string + "'");
            return (Byte) properties.get(string);
        }

        public double getDoubleProperty(String string) throws JMSException {
            logger.info("Double property get '" + string + "'");
            return (Double) properties.get(string);
        }

        public float getFloatProperty(String string) throws JMSException {
            logger.info("Float property get '" + string + "'");
            return (Float) properties.get(string);
        }

        public int getIntProperty(String string) throws JMSException {
            logger.info("Int property get '" + string + "'");
            return (Integer) properties.get(string);
        }

        public String getJMSCorrelationID() throws JMSException {
            return correlationID;
        }

        public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
            return getJMSCorrelationID().getBytes();
        }

        public int getJMSDeliveryMode() throws JMSException {
            return 0;
        }

        public Destination getJMSDestination() throws JMSException {
            return null;
        }

        public long getJMSExpiration() throws JMSException {
            return 0;
        }

        public String getJMSMessageID() throws JMSException {
            return null;
        }

        public int getJMSPriority() throws JMSException {
            return 0;
        }

        public boolean getJMSRedelivered() throws JMSException {
            return false;
        }

        public Destination getJMSReplyTo() throws JMSException {
            return replyTo;
        }

        public long getJMSTimestamp() throws JMSException {
            return 0;
        }

        public String getJMSType() throws JMSException {
            return null;
        }

        public long getLongProperty(String string) throws JMSException {
            logger.info("Long property get '" + string + "'");
            return (Long) properties.get(string);
        }

        public Object getObjectProperty(String string) throws JMSException {
            logger.info("Object property get '" + string + "'");
            return properties.get(string);
        }

        public Enumeration getPropertyNames() throws JMSException {
            return Collections.enumeration(properties.keySet());
        }

        public short getShortProperty(String string) throws JMSException {
            logger.info("Short property get '" + string + "'");
            return (Short) properties.get(string);
        }

        public String getStringProperty(String string) throws JMSException {
            logger.info("String property get '" + string + "'");
            return (String) properties.get(string);
        }

        public boolean propertyExists(String string) throws JMSException {
            return properties.containsKey(string);
        }

        public void setBooleanProperty(String string, boolean b) throws JMSException {
            logger.info("Boolean property set '" + string + "' = '" + b + "'.");
            properties.put(string,b);
        }

        public void setByteProperty(String string, byte b) throws JMSException {
            logger.info("Byte property set '" + string + "' = '" + b + "'.");
            properties.put(string,b);
        }

        public void setDoubleProperty(String string, double v) throws JMSException {
            logger.info("Double property set '" + string + "' = '" + v + "'.");
            properties.put(string,v);
        }

        public void setFloatProperty(String string, float v) throws JMSException {
            logger.info("Float property set '" + string + "' = '" + v + "'.");
            properties.put(string,v);
        }

        public void setIntProperty(String string, int i) throws JMSException {
            logger.info("Int property set '" + string + "' = '" + i + "'.");
            properties.put(string,i);
        }

        public void setJMSCorrelationID(String string) throws JMSException {
            correlationID = string;
        }

        public void setJMSCorrelationIDAsBytes(byte[] bytes) throws JMSException {
            setJMSCorrelationID(new String(bytes));
        }

        public void setJMSDeliveryMode(int i) throws JMSException {
        }

        public void setJMSDestination(Destination destination) throws JMSException {
        }

        public void setJMSExpiration(long l) throws JMSException {
        }

        public void setJMSMessageID(String string) throws JMSException {
        }

        public void setJMSPriority(int i) throws JMSException {
        }

        public void setJMSRedelivered(boolean b) throws JMSException {
        }

        public void setJMSReplyTo(Destination destination) throws JMSException {
            replyTo = destination;
        }

        public void setJMSTimestamp(long l) throws JMSException {
        }

        public void setJMSType(String string) throws JMSException {
        }

        public void setLongProperty(String string, long l) throws JMSException {
            logger.info("Long property set '" + string + "' = '" + l + "'.");
            properties.put(string,l);
        }

        public void setObjectProperty(String string, Object object) throws JMSException {
            logger.info("Object property set '" + string + "' = '" + object + "'.");
            properties.put(string,object);
        }

        public void setShortProperty(String string, short i) throws JMSException {
            logger.info("Short property set '" + string + "' = '" + i + "'.");
            properties.put(string,i);
        }

        public void setStringProperty(String string, String string1) throws JMSException {
            logger.info("String property set '" + string + "' = '" + string1 + "'.");
            properties.put(string,string1);
        }

        public String getText() throws JMSException {
            return messageText;
        }

        public void setText(String string) throws JMSException {
            messageText = string;
        }
    }
}
