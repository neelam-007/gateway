package com.l7tech.gui.util;

/**
 * Interface implemented by components that are capable of showing modeless feedback.
 * Examples include SquigglyTextArea and SquigglyTextField.
 */
public interface ModelessFeedback {
    /**
     * Check feedback that is being made available by this componenet.
     *
     * @return the feedback currently being made available, or null.
     */
    String getModelessFeedback();

    /**
     * Sets feedback to be made available modelessly by this componenet.
     *
     * @param feedback  the feedback to make available, or null to clear modeless feedback.
     */
    void setModelessFeedback(String feedback);
}
