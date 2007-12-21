package org.nicegamepads;

import net.java.games.input.Component;

/**
 * Encapsulated information about the state of a component.
 * 
 * @author Andrew Hayden
 */
final class ComponentState
{
    /**
     * The component to which this state applies.
     */
    final Component component;

    /**
     * The type of the component, stored for convenience.
     */
    final ComponentType type;

    /**
     * Timestamp at which this state was acquired.
     */
    long currentTimestamp = -1L;

    /**
     * Value of the component at the time this state was acquired.
     */
    float currentValue = 0f;

    /**
     * The timestamp at which the last polling was completed.
     */
    long lastTimestamp = -1L;

    /**
     * The value of the component at the last polling time.
     */
    float lastValue = 0f;

    /**
     * The last time the turbo timer started, if any.
     */
    long lastTurboTimerStart = -1L;

    /**
     * Constructs a new state to represent the specified component.
     * 
     * @param component the component
     */
    ComponentState(Component component)
    {
        this.component = component;
        this.type = ControllerUtils.getComponentType(component);
    }

    /**
     * Archives the current value and timestamp and sets the new values.
     * 
     * @param value the new value
     * @param timestamp the new timestamp
     * @param canPerpetuateTurbo whether or not the value represents a value
     * that starts or perptuates the turbo state
     */
    final void newValue(float value, long timestamp, boolean canPerpetuateTurbo)
    {
        this.lastTimestamp = this.currentTimestamp;
        this.lastValue = this.currentValue;
        this.currentValue = value;
        this.currentTimestamp = timestamp;

        if (canPerpetuateTurbo)
        {
            if (lastTurboTimerStart == -1)
            {
                // Haven't started turbo timer yet.  Start it.
                lastTurboTimerStart = timestamp;
            }
        }
        else
        {
            // Turbo timer must be cleared since this value doesn't
            // represent a value that can apply to turbo
            lastTurboTimerStart = -1;
        }
    }
}