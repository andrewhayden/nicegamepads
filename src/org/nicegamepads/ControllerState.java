package org.nicegamepads;

import java.util.List;

import net.java.games.input.Component;
import net.java.games.input.Controller;

/**
 * Encapsulates the state of a controller.
 * 
 * @author Andrew Hayden
 */
final class ControllerState
{
    /**
     * The controller associated with this state.
     */
    final Controller controller;

    /**
     * All of the component states associated with this controller state,
     * possibly recursively expanded (according to the constructor parameters
     * provided)
     */
    final ComponentState[] componentStates;

    /**
     * Constructs a new controller state for the specified controller.
     * 
     * @param controller the controller to create state for
     * @param deep whether or not the recursively descend into all
     * subcontrollers for the purpose of locating components;
     * if <code>true</code>, the controller state represents the state of
     * all of its components as well as the components of all of its
     * subcontrollers, recursively expanded; otherwise, the controller state
     * represents only the state of the components that are the immediate
     * children of the specified controller
     */
    ControllerState(Controller controller, boolean deep)
    {
        this.controller = controller;
        List<Component> allComponents =
            ControllerUtils.getComponents(controller, deep);

        // Create component states.
        componentStates = new ComponentState[allComponents.size()];
        int index = 0;
        for (Component component : allComponents)
        {
            componentStates[index++] = new ComponentState(component);
        }
    }
}
