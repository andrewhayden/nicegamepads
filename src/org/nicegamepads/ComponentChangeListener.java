package org.nicegamepads;

/**
 * Interface for entities wishing to be notified of fine-grained changes to
 * a components' values.
 * <p>
 * Warning: due to the nature of input devices, implementers should be
 * prepared to handle a <em>very large</em> number of events per second
 * (potentially as many as there are polling intervals in a given second).
 * 
 * @author Andrew Hayden
 */
public interface ComponentChangeListener
{
    /**
     * Invoked whenever the value of a component changes.
     * 
     * @param event event details
     */
    public abstract void valueChanged(ComponentEvent event);
}
