package com.l7tech.console.util;

/**
 * The <code>History</code> is implemented by classes that wish to
 * keep track of objects.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public interface History {
    /**
     * Add the object to the <code>History</code>.
     * <p>
     * Implementations may remove the older element of the History if
     * their maximum size is reached.
     *
     * @param o the object to add to the
     */
    void add(Object o);

    /**
     * get the array of history entries.
     * @return the array of history entries.
     */
    Object[] getEntries();

    /*
    * Set the history size
    * Setting this should cause the history to be updated immediately
    * such that the number of values returned by getEntries() is equal to maxSize
    * */
    public void setMaxSize(int maxSize);

    /*
    * @return how large the history is allowed for this instance of History
    * */
    public int getMaxSize();
}
