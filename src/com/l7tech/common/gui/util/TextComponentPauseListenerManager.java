/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.util;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public class TextComponentPauseListenerManager {
    public static final int TIMER_PERIOD = 200;

    private static class TextComponentPauseNotifier
            implements DocumentListener, PropertyChangeListener, ComponentListener,
            InputMethodListener {

        public TextComponentPauseNotifier( JTextComponent component, int notifyDelay ) {
            _component = component;
            _notifyDelay = notifyDelay;

            component.addPropertyChangeListener( "enabled", this );
            component.addComponentListener( this );
            component.getDocument().addDocumentListener( this );

            if ( component.isEnabled() && component.isVisible() ) start();
        }

        public JTextComponent getComponent() {
            return _component;
        }

        public void addPauseListener( PauseListener pl ) {
            _listeners.add( pl );
        }

        public void removePauseListener( PauseListener pl ) {
            _listeners.remove( pl );
        }

        private void notifyPause( long howlong ) {
            for (Iterator i = _listeners.iterator(); i.hasNext();) {
                PauseListener listener = (PauseListener) i.next();
                listener.textEntryPaused( _component, howlong );
            }
        }

        private void notifyResume() {
            for (Iterator i = _listeners.iterator(); i.hasNext();) {
                PauseListener listener = (PauseListener) i.next();
                listener.textEntryResumed( _component );
            }
        }

        public void start() {
            if ( _task != null ) _task.cancel();
            _task = new MyTimerTask();
            _timer.scheduleAtFixedRate( _task, 0, TIMER_PERIOD );
            _updated = System.currentTimeMillis();
        }

        public void updated() {
            _updated = System.currentTimeMillis();
            if ( _paused ) notifyResume();
            _paused = false;
        }

        public void stop() {
            if ( _task != null ) _task.cancel();
            _paused = true;
        }

        public void insertUpdate(DocumentEvent e) {
            updated();
        }

        public void removeUpdate(DocumentEvent e) {
            updated();
        }

        public void changedUpdate(DocumentEvent e) {
            updated();
        }

        public void propertyChange(PropertyChangeEvent evt) {
            String pname = evt.getPropertyName();
            if ( "editable".equals( pname ) ) {
                Boolean b = (Boolean)evt.getNewValue();
                if ( b.booleanValue() )
                    start();
                else
                    stop();
            }
        }

        public void componentShown(ComponentEvent e) {
            start();
        }

        public void componentHidden(ComponentEvent e) {
            stop();
        }

        private final JTextComponent _component;
        private List _listeners = new ArrayList();
        private MyTimerTask _task;
        private long _updated;
        private long _notifyDelay;
        private boolean _paused;

        public void inputMethodTextChanged(InputMethodEvent event) {
            updated();
        }

        public void caretPositionChanged(InputMethodEvent event) {
            // Don't care
        }

        public void componentResized(ComponentEvent e) {
            // Don't care
        }

        public void componentMoved(ComponentEvent e) {
            // Don't care
        }

        private class MyTimerTask extends TimerTask {
            public void run() {
                long now = System.currentTimeMillis();
                long howlong = now - _updated;
                if ( howlong >= _notifyDelay ) {
                    _paused = true;
                    notifyPause(howlong);
                    stop();
                    start();
                }
            }
        }
    }

    public static void registerPauseListener( JTextComponent component, PauseListener pl, int notifyDelay ) {
        TextComponentPauseNotifier holder = new TextComponentPauseNotifier( component, notifyDelay );
        holder.addPauseListener( pl );
    }

    private static Timer _timer = new Timer(true);
}
