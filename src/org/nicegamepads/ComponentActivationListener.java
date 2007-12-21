package org.nicegamepads;

/**
 * Interface for entities wishing to be notified when a component is
 * activated or deactivated.
 * <p>
 * A component becomes "activated" whenever its value becomes equal to one
 * of the values bound to the associated {@link ComponentConfiguration}.
 * A component becomes "deactivated" whenever its value ceases to be equal
 * to such a value.
 * If a component moves from one bound value to another in a single polling
 * interval, two events should be fired (one for the deactivation of the
 * component at its previous value, and one for the activation of the
 * component at its new value).
 * 
 * @author Andrew Hayden
 */
public interface ComponentActivationListener
{
    /**
     * Invoked whenever a component becomes activated.  A component becomes
     * activated when its value becomes equal to one of the values bound
     * in the associated {@link ComponentConfiguration}.
     * <p>
     * In this case the field {@link ComponentEvent#currentValueId} contains
     * the id of the previously-active value.
     * 
     * @param event the event details
     */
    public abstract void componentActivated(ComponentEvent event);

    /**
     * Invoked whenever a component becomes deactivated.  A component becomes
     * deactivated when its value is no longer equal to one of the values bound
     * in the associated {@link ComponentConfiguration}.
     * <p>
     * In this case the field {@link ComponentEvent#previousValueId} contains
     * the id of the previously-active value.
     * 
     * @param event the event details
     */
    public abstract void componentDeactivated(ComponentEvent event);
}
