package com.l7tech.policy.assertion;

/**
 * Enum for <code>javax.jms.DeliveryMode</code>'s
 */
public enum JmsDeliveryMode {

    //- PUBLIC

    /**
     * As per <code>javax.jms.DeliveryMode.PERSISTENT</code>
     */
    PERSISTENT( "Persistent", 2 ),

    /**
     * As per <code>javax.jms.DeliveryMode.NON_PERSISTENT</code>
     */
    NON_PERSISTENT( "Non-Persistent", 1 );

    /**
     * Get the display name for this mode.
     *
     * @return The display name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the value for this mode.
     *
     * @return The value.
     */
    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name;
    }

    //- PRIVATE

    private final String name;
    private final int value;

    private JmsDeliveryMode( final String name,
                             final int value ) {
        this.name = name;
        this.value = value;
    }
}
