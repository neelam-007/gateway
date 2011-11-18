/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gui.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.Timer;

/**
 * @author alex
 * @version $Revision$
 */
public class TextComponentPauseListenerManager {
    public static final int TIMER_PERIOD = 200;

    private static class TextComponentPauseNotifier
      implements DocumentListener, PropertyChangeListener, ComponentListener,
      InputMethodListener {

        public TextComponentPauseNotifier(@NotNull final JTextComponent component,
                                          final int notifyDelay,
                                          final boolean focusControlled,
                                          @Nullable final FocusListener focusCallbackListener) {
            _component = component;
            _notifyDelay = notifyDelay;

            component.addPropertyChangeListener("enabled", this);
            component.addComponentListener(this);
            component.getDocument().addDocumentListener(this);

            if (focusControlled) {
                this._component.addFocusListener(new FocusListener() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        start();
                        if (focusCallbackListener != null) {
                            focusCallbackListener.focusGained(e);
                        }
                    }

                    @Override
                    public void focusLost(FocusEvent e) {
                        stop();
                        if (focusCallbackListener != null) {
                            focusCallbackListener.focusLost(e);
                        }
                    }
                });
            } else {
                if (component.isEnabled() && component.isVisible()) start();
            }
        }

        private TextComponentPauseNotifier(final JTextComponent _component,
                                           final int notifyDelay) {
            this(_component, notifyDelay, false, null);
        }

        public JTextComponent getComponent() {
            return _component;
        }

        public void addPauseListener(PauseListener pl) {
            _listeners.add(pl);
        }

        public void removePauseListener(PauseListener pl) {
            _listeners.remove(pl);
        }

        private void notifyPause(long howlong) {
            for (Object _listener : _listeners) {
                PauseListener listener = (PauseListener) _listener;
                listener.textEntryPaused(_component, howlong);
            }
        }

        private void notifyResume() {
            for (Object _listener : _listeners) {
                PauseListener listener = (PauseListener) _listener;
                listener.textEntryResumed(_component);
            }
        }

        public synchronized void start() {
            if (_task != null) _task.cancel();
            _task = new MyTimerTask();
            // bugzilla #2615
            //_timer.scheduleAtFixedRate(_task, 0, TIMER_PERIOD);
            (new Timer(true)).scheduleAtFixedRate(_task, 0, TIMER_PERIOD);
            _updated = System.currentTimeMillis();
        }

        public synchronized void updated() {
            _updated = System.currentTimeMillis();
            if (_paused) notifyResume();
            _paused = false;
        }

        public synchronized void stop() {
            if (_task != null) _task.cancel();
            _paused = true;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updated();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updated();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updated();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String pname = evt.getPropertyName();
            if ("editable".equals(pname)) {
                Boolean b = (Boolean)evt.getNewValue();
                if (b)
                    start();
                else
                    stop();
            }
        }

        @Override
        public void componentShown(ComponentEvent e) {
            start();
        }

        @Override
        public void componentHidden(ComponentEvent e) {
            stop();
        }

        private final JTextComponent _component;
        private List<PauseListener> _listeners = new ArrayList<PauseListener>();
        private MyTimerTask _task;
        private volatile long _updated;
        private final long _notifyDelay;
        private boolean _paused;

        @Override
        public void inputMethodTextChanged(InputMethodEvent event) {
            updated();
        }

        @Override
        public void caretPositionChanged(InputMethodEvent event) {
            // Don't care
        }

        @Override
        public void componentResized(ComponentEvent e) {
            // Don't care
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            // Don't care
        }

        private class MyTimerTask extends TimerTask {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                final long howlong = now - _updated;
                if (howlong >= _notifyDelay && !_paused) {
                    synchronized (TextComponentPauseNotifier.this) {
                        _paused = true;
                    }

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            notifyPause(howlong);
                            stop();
                            start();
                        }
                    });
                }
            }
        }
    }

    /**
     * Warning - be careful of usages where pause events will be fired before the text component's panel / dialog
     * is ready for usage. See WizardStepPanel canAdvance(). In these cases use {@link #registerPauseListenerWhenFocused(javax.swing.text.JTextComponent, PauseListener, int)}
     * instead.
     *
     * Invoke only from the UI thread.
     * @param component The text component to register.
     * @param pl The listener to invoke when paused.
     * @param notifyDelay The delay between notifications.
     */
    public static void registerPauseListener(JTextComponent component, PauseListener pl, int notifyDelay) {
        TextComponentPauseNotifier holder = new TextComponentPauseNotifier(component, notifyDelay);
        holder.addPauseListener(pl);
    }

    /**
     * Register a pause listener which is only active when the text component registered is focused.
     * <p/>
     * Note: If you want to catch a validation error with this PauseListener, then the notifyDelay must either be very
     * short or your code must check independently before accepting input.
     * <p/>
     * Invoke from UI thread.
     * <p/>
     * Note: A text component may only have a single focus listener. Use when the text component to focus on has other
     * focus requirements which require callback to the supplied FocusListener.
     *
     * @param component             The text component to register.
     * @param pl                    The listener to invoke when paused.
     * @param notifyDelay           The delay between notifications.
     * @param focusCallbackListener callback listener for focusGained and focusLost events.
     */
    public static void registerPauseListenerWhenFocused(JTextComponent component,
                                                        PauseListener pl,
                                                        int notifyDelay,
                                                        @Nullable FocusListener focusCallbackListener) {
        TextComponentPauseNotifier holder = new TextComponentPauseNotifier(component, notifyDelay, true, focusCallbackListener);
        holder.addPauseListener(pl);
    }

    /**
     * Register a pause listener which is only active when the text component registered is focused.
     *
     * @param component The text component to register.
     * @param pl The listener to invoke when paused.
     * @param notifyDelay The delay between notifications.
     */
    public static void registerPauseListenerWhenFocused(JTextComponent component,
                                                        PauseListener pl,
                                                        int notifyDelay) {
        registerPauseListenerWhenFocused(component, pl, notifyDelay, null);
    }

    // bugzilla #2615
    //private static Timer _timer = new Timer(true);
}
