package org.nicegamepads;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.java.games.input.Component;

/**
 * Encapsulates the state of a controller.
 * 
 * @author Andrew Hayden
 */
public final class ControllerState
{
    /**
     * All of the component states associated with this controller state,
     * possibly recursively expanded (according to the constructor parameters
     * provided)
     */
    final ComponentState[] componentStates;

    /**
     * The configuration associated with this state.
     */
    final ControllerConfiguration configuration;

    /**
     * Lazily-initialized mapping of states by their components.
     */
    private final Map<Component, ComponentState>
        cachedStatesByComponent;

    /**
     * The last time at which this controller state was completely refreshed,
     * in milliseconds since the epoch.
     */
    volatile long timestamp = -1L;

    /**
     * Constructs a new controller state for the specified controller.
     * 
     * @param configuration the configuration to create state for
     * @param deep whether or not the recursively descend into all
     * subcontrollers for the purpose of locating components;
     * if <code>true</code>, the controller state represents the state of
     * all of its components as well as the components of all of its
     * subcontrollers, recursively expanded; otherwise, the controller state
     * represents only the state of the components that are the immediate
     * children of the specified controller
     */
    ControllerState(ControllerConfiguration configuration, boolean deep)
    {
        this.configuration = configuration;
        List<Component> allComponents =
            ControllerUtils.getComponents(
                    configuration.getController(), deep);

        // Create component states.
        componentStates = new ComponentState[allComponents.size()];
        Map<Component, ComponentState> tempMap =
            new HashMap<Component, ComponentState>();
        int index = 0;
        for (Component component : allComponents)
        {
            componentStates[index] = new ComponentState(component);
            tempMap.put(component, componentStates[index]);
            index++;
        }
        cachedStatesByComponent = Collections.unmodifiableMap(tempMap);
    }

    /**
     * Constructs an independent copy of the specified source object.
     * <p>
     * This method creates a deep copy.  It is entirely independent of the
     * source in all respects.
     * 
     * @param source the source to copy from
     */
    ControllerState(ControllerState source)
    {
        configuration = new ControllerConfiguration(source.configuration);
        timestamp = source.timestamp;
        componentStates = new ComponentState[source.componentStates.length];
        Map<Component, ComponentState> tempMap =
            new HashMap<Component, ComponentState>();
        for (int index=0; index<componentStates.length; index++)
        {
            componentStates[index] =
                new ComponentState(source.componentStates[index]);
            tempMap.put(
                    componentStates[index].component, componentStates[index]);
        }
        cachedStatesByComponent = Collections.unmodifiableMap(tempMap);
    }

    /**
     * Returns the state for the specified component within this controller
     * state.
     * 
     * @param component the component whose state should be retrieved
     * @return the state of the component
     * @throws NoSuchComponentException if the specified component is not
     * part of the controller associated with this state
     */
    public final ComponentState getComponentState(Component component)
    {
        ComponentState state = cachedStatesByComponent.get(component);
        if (state == null)
        {
            throw new NoSuchComponentException(
                    "Component does not exist in the controller "
                    + "associated with this state.");
        }
        return state;
    }

    /**
     * Returns the last time at which this controller state was completely
     * refreshed, in milliseconds since the epoch.
     * 
     * @return the time
     */
    public final long getTimestamp()
    {
        return timestamp;
    }
}