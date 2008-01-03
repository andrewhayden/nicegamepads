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
    final ComponentType componentType;

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
        this.componentType = ControllerUtils.getComponentType(component);
    }

    /**
     * Constructs an independent copy of the specified state.
     * 
     * @param source the source to copy from
     */
    ComponentState(ComponentState source)
    {
        this.component = source.component;
        this.componentType = source.componentType;
        this.currentTimestamp = source.currentTimestamp;
        this.currentValue = source.currentValue;
        this.lastTimestamp = source.lastTimestamp;
        this.lastTurboTimerStart = source.lastTurboTimerStart;
        this.lastValue = source.lastValue;
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

    @Override
    public final String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(ComponentState.class.getName());
        buffer.append(": [");
        buffer.append("component=");
        buffer.append(component);
        buffer.append(", componentType=");
        buffer.append(componentType);
        buffer.append(", currentValue=");
        buffer.append(currentValue);
        buffer.append(", lastValue=");
        buffer.append(lastValue);
        buffer.append(", currentTimestamp=");
        buffer.append(currentTimestamp);
        buffer.append(", lastTurboTimerStart=");
        buffer.append(lastTurboTimerStart);
        buffer.append("]");
        return buffer.toString();
    }

    /**
     * Returns the component to which this state applies.
     * 
     * @return the component to which this state applies.
     */
    public final Component getComponent()
    {
        return component;
    }

    /**
     * Returns the type of the component, for convenience.
     * 
     * @return the type of the component, for convenience.
     */
    public final ComponentType getComponentType()
    {
        return componentType;
    }

    /**
     * Returns the timestamp at which this state was acquired, in milliseconds
     * since the epoch.
     * <p>
     * If the component has never been polled, the value is -1.
     * 
     * @return the timestamp at which this state was acquired, in milliseconds
     * since the epoch.
     */
    public final long getCurrentTimestamp()
    {
        return currentTimestamp;
    }

    /**
     * Returns the value of the component at the time this state was acquired.
     * <p>
     * If the component has never been polled, the value is 0.
     * 
     * @return the value of the component at the time this state was acquired.
     */
    public final float getCurrentValue()
    {
        return currentValue;
    }

    /**
     * Returns the timestamp at which the last polling was completed,
     * in milliseconds since the epoch.
     * <p>
     * If the component has never been polled, the value is -1.
     * 
     * @return the timestamp at which the last polling was completed,
     * in milliseconds since the epoch.
     */
    public final long getLastTimestamp()
    {
        return lastTimestamp;
    }

    /**
     * Returns the value of the component at the last polling time.
     * <p>
     * If the component has never been polled, the value is zero.
     * 
     * @return the value of the component at the last polling time.
     */
    public final float getLastValue()
    {
        return lastValue;
    }

    /**
     * Returns the last time the turbo timer started, if any.
     * <p>
     * If the timer has never started, the value is -1.
     * 
     * @return the last time the turbo timer started, if any.
     */
    public final long getLastTurboTimerStart()
    {
        return lastTurboTimerStart;
    }
}