package com.l7tech.gui.widgets;

import com.l7tech.gui.util.ModelessFeedback;

import java.awt.*;

/**
 * Common interface for squiggly fields such as {@link SquigglyTextField} and {@link SquigglyTextArea}.
 */
public interface SquigglyField extends ModelessFeedback {
    int NONE = -2;
    int ALL = -1;

    int getBegin();

    int getEnd();

    void setRange( int begin, int end );

    void setAll();

    void setNone();

    Color getColor();

    void setColor(Color color);

    void setSquiggly();

    void setDotted();

    void setStraight();
}
