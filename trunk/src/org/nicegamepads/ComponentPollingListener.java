package org.nicegamepads;

/**
 * Interface for entities wishing to be notified about every single polling
 * event that occurs for a component.
 * <p>
 * This is the finest possible level of listener.  These listeners should
 * expect to be invoked every single time a polling interval elapses,
 * regardless of whether or not the value of the component has changed.
 * 
 * @author Andrew Hayden
 */
public interface ComponentPollingListener
{
    /**
     * Invoked every time a component is polled.
     * 
     * @param event event details
     */
    public abstract void componentPolled(ComponentEvent event);
}
