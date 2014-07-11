package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import org.apache.mina.core.buffer.IoBuffer;
import org.xml.sax.SAXException;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * This is from the Apache Vysper project (org.apache.vysper.xml.sax.impl).
 * Trying to minimize changes from the original file so that we could switch
 * to using the vysper library.
 */
public class XMLTokenizer {
    private static enum State {
        START,
        IN_TAG,
        IN_STRING,
        IN_DOUBLE_ATTRIBUTE_VALUE,
        IN_SINGLE_ATTRIBUTE_VALUE,
        IN_TEXT,
        CLOSED
    }

    private static final char NO_CHAR = (char)-1;

    private State state = State.START;
    private final IoBuffer buffer = IoBuffer.allocate(16).setAutoExpand(true);

    private static final CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();

    private TokenHandler tokenHandler;

    public XMLTokenizer(TokenHandler tokenHandler) {
        this.tokenHandler = tokenHandler;
    }

    protected void parse(IoBuffer in) throws SAXException {
        while(in.hasRemaining() && state != State.CLOSED/* && !tokenHandler.isDone()*/) {
            char c = (char)in.get();

            if(state == State.START) {
                if(c == '<') {
                    emit(c, in);
                    state = State.IN_TAG;
                } else {
                    state = State.IN_TEXT;
                    buffer.put((byte)c);
                }
            } else if(state == State.IN_TEXT) {
                if(c == '<') {
                    emit(in);
                    emit(c, in);
                    state = State.IN_TAG;
                } else {
                    buffer.put((byte)c);
                }
            } else if(state == State.IN_TAG) {
                if(c == '>') {
                    emit(c, in);
                    state = State.START;
                } else if(c == '"') {
                    emit(c, in);
                    state = State.IN_DOUBLE_ATTRIBUTE_VALUE;
                } else if(c == '\'') {
                    emit(c, in);
                    state = State.IN_SINGLE_ATTRIBUTE_VALUE;
                } else if(c == '-') {
                    emit(c, in);
                } else if(isControlChar(c)) {
                    emit(c, in);
                } else if(Character.isWhitespace(c)) {
                    buffer.clear();
                } else {
                    state = State.IN_STRING;
                    buffer.put((byte)c);
                }
            } else if(state == State.IN_STRING) {
                if(c == '>') {
                    emit(in);
                    emit(c, in);
                    state = State.START;
                } else if(isControlChar(c)) {
                    emit(in);
                    emit(c, in);
                    state = State.IN_TAG;
                } else if(Character.isWhitespace(c)) {
                    emit(in);
                    state = State.IN_TAG;
                } else {
                    buffer.put((byte)c);
                }
            } else if(state == State.IN_DOUBLE_ATTRIBUTE_VALUE) {
                if(c == '"') {
                    emit(in);
                    emit(c, in);
                    state = State.IN_TAG;
                } else {
                    buffer.put((byte)c);
                }
            } else if(state == State.IN_SINGLE_ATTRIBUTE_VALUE) {
                if(c == '\'') {
                    emit(in);
                    emit(c, in);
                    state = State.IN_TAG;
                } else {
                    buffer.put((byte)c);
                }
            }
        }
    }

    private boolean isControlChar(char c) {
        return c == '<' || c == '>' || c == '!' || c == '/' || c == '?' || c == '=';
    }

    private void emit(char token, IoBuffer in) throws SAXException {
        tokenHandler.token(token, null);
    }

    private void emit(IoBuffer in) throws SAXException {
        try {
            buffer.flip();
            tokenHandler.token(NO_CHAR, buffer.getString(decoder));
            buffer.clear();
        } catch(CharacterCodingException e) {
            throw new SAXException(e);
        }
    }
}
