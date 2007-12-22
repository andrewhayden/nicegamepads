package org.nicegamepads;

import net.java.games.input.Component;
import net.java.games.input.Controller;

/**
 * Encapsulates information about an event from a component.
 * 
 * @author Andrew Hayden
 */
public class ComponentEvent
{
    /**
     * The parent controller in which the source component resides, if
     * known; otherwise, <code>null</code>.
     * <p>
     * If set, this is always the immediate parent controller of the
     * component.
     */
    public final Controller sourceController;

    /**
     * The component that generated the event.
     */
    public final Component sourceComponent;

    /**
     * The type of the source component, if the source component is
     * not <code>null</code>.
     */
    public final ComponentType sourceComponentType;

    /**
     * The user-defined ID for the source component, if any; otherwise,
     * {@link Integer#MIN_VALUE}.
     */
    public final int userDefinedComponentId;

    /**
     * The current value of the component at the time this event was fired,
     * or {@link Float#NaN} if there is no applicable value.
     */
    public final float currentValue;

    /**
     * The current user-defined value ID bound to the current value, if any;
     * otherwise, {@link Integer#MIN_VALUE}.
     */
    public final int currentValueId;

    /**
     * The value of the component at the previous time the source component,
     * was polled, or {@link Float#NaN} if there is no applicable value.
     */
    public final float previousValue;

    /**
     * The current user-defined value ID bound to the previous value, if any;
     * otherwise, {@link Integer#MIN_VALUE}.
     */
    public final int previousValueId;

    /**
     * Constructs a new component event.
     * 
     * @param topLevelSourceController
     * @param sourceComponent
     * @param userDefinedComponentId
     * @param currentValue
     * @param currentValueId
     * @param previousValue
     * @param previousValueId
     */
    public ComponentEvent(Controller topLevelSourceController,
            Component sourceComponent, int userDefinedComponentId,
            float currentValue, int currentValueId, float previousValue,
            int previousValueId)
    {
        super();
        this.sourceController = topLevelSourceController;
        this.sourceComponent = sourceComponent;
        this.userDefinedComponentId = userDefinedComponentId;
        this.currentValue = currentValue;
        this.currentValueId = currentValueId;
        this.previousValue = previousValue;
        this.previousValueId = previousValueId;

        if (sourceComponent != null)
        {
            this.sourceComponentType =
                ControllerUtils.getComponentType(sourceComponent);
        }
        else
        {
            this.sourceComponentType = null;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(ComponentEvent.class.getName());
        buffer.append(": [");
        buffer.append("sourceController=");
        buffer.append(sourceController);
        buffer.append(", sourceComponent=");
        buffer.append(sourceComponent);
        buffer.append(", sourceComponentType=");
        buffer.append(sourceComponentType);
        buffer.append(", userDefinedComponentId=");
        buffer.append(userDefinedComponentId);
        buffer.append(", previousValue=");
        buffer.append(previousValue);
        buffer.append(", previousValueId=");
        buffer.append(previousValueId);
        buffer.append(", currentValue=");
        buffer.append(currentValue);
        buffer.append(", currentValueId=");
        buffer.append(currentValueId);
        buffer.append("]");
        return buffer.toString();
    }
}