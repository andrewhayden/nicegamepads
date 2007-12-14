package org.nicegamepads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.java.games.input.Component;
import net.java.games.input.Controller;

/**
 * Represents the configuration for a controller.
 * <p>
 * A controller may have subcontrollers embedded within it.
 * 
 * @author Andrew Hayden
 */
public class ControllerConfiguration
{
    /**
     * Configurations for each control in the controller.
     */
    private ComponentConfiguration[] componentConfigurations;

    /**
     * Configurations for each subcontroller that is a direct child of this
     * controller configuration.
     */
    private ControllerConfiguration[] subControllerConfigurations;

    /**
     * Type code for the controller, can be used as a sanity check during
     * loading.
     */
    private int controllerTypeCode;

    /**
     * Creates a configuration comaptible with, but not specifically tied to,
     * the specified instance of controller.
     * <p>
     * The controller is used solely for determining the various attributes
     * that need to be persisted.  That is, it is used primarily for
     * examination of hardware.  A unique type code for the controller is
     * inferred and kept as a sanity check for loading the configuration
     * in the future.
     * 
     * @param controller the controller to create a compatible configuration
     * for
     */
    ControllerConfiguration(Controller controller)
    {
        this.controllerTypeCode = ControllerUtils.generateTypeCode(controller);
        Component[] components = controller.getComponents();
        if (components != null)
        {
            componentConfigurations = new ComponentConfiguration[components.length];
            for (int index=0; index<components.length; index++)
            {
                componentConfigurations[index] = new ComponentConfiguration();
            }
        }
        else
        {
            componentConfigurations = new ComponentConfiguration[0];
        }

        // Create child configs
        Controller[] subControllers = controller.getControllers();
        if (subControllers != null)
        {
            subControllerConfigurations =
                new ControllerConfiguration[subControllers.length];
            for (int index=0; index<subControllers.length; index++)
            {
                subControllerConfigurations[index] =
                    new ControllerConfiguration(subControllers[index]);
            }
        }
        else
        {
            subControllerConfigurations = new ControllerConfiguration[0];
        }
    }

    /**
     * Performs a breadth-first traversal of the entire controller hierarchy,
     * recording the component configurations for each controller before
     * descending into any subcontrollers.
     * 
     * @param configuration the configuration to start the traversal in
     * @return a breadth-first listing of all component configurations
     * found in or under this controller configuration
     */
    final static List<ComponentConfiguration>
        enumerateAllComponentConfigurations(
                ControllerConfiguration configuration)
    {
        // Do a traversal of the entire tree of configurations, breadth-first
        List<ComponentConfiguration> allConfigs =
            new ArrayList<ComponentConfiguration>();
        enumerationHelper(configuration, allConfigs);
        return allConfigs;
    }

    /**
     * Recurses into subcontroller configs, enumerating their children in
     * a breadth-first traversal.
     * 
     * @param allConfigs list in which to place results
     */
    private final static void enumerationHelper(
            ControllerConfiguration target,
            List<ComponentConfiguration> allConfigs)
    {
        for (int x=0; x<target.componentConfigurations.length; x++)
        {
            allConfigs.add(target.componentConfigurations[x]);
        }
        for (int x=0; x<target.subControllerConfigurations.length; x++)
        {
            enumerationHelper(target.subControllerConfigurations[x],
                    allConfigs);
        }
    }

    /**
     * Saves this configuration to a mapping of (key,value) pairs
     * in an unambiguous manner suitable for user in a Java properties file.
     * <p>
     * The specified prefix, with an added trailing "." character, is
     * prepended to the names of all properties written by this method.
     * 
     * @param prefix the prefix to use for creating the property keys
     * @param destination optionally, a map into which the properties should
     * be written; if <code>null</code>, a new map is created and returned.
     * Any existing entries with the same names are overwritten.
     * @return if <code>destination</code> was specified, the reference to
     * that same object (which now contains this configuration's (key,value)
     * pairs); otherwise, a new {@link Map} containing this configuration's
     * (key,value) pairs
     */
    final Map<String, String> saveToProperties(
            String prefix, Map<String,String> destination)
    {
        if (destination == null)
        {
            destination = new HashMap<String, String>();
        }

        destination.put(prefix + ".controllerTypeCode",
                Integer.toString(controllerTypeCode));
        destination.put(prefix + ".numComponents",
                Integer.toString(componentConfigurations.length));
        destination.put(prefix + ".numSubControllers",
                Integer.toString(subControllerConfigurations.length));

        // Write out all components.
        for (int x=0; x<componentConfigurations.length; x++)
        {
            componentConfigurations[x].saveToProperties(
                    prefix + ".component" + x, destination);
        }

        for (int x=0; x<subControllerConfigurations.length; x++)
        {
            subControllerConfigurations[x].saveToProperties(
                    prefix + ".subController" + x, destination);
        }

        return destination;
    }

    /**
     * Loads this configuration from Java properties.
     * 
     * @param prefix the prefix to use for retrieving the property keys,
     * which should be the same as when {@link #saveToProperties(String, Map)}
     * was originally called to save the configuration
     * @param source properties to read from
     * @throws ConfigurationException if any of the required keys or values
     * are missing or if any of the values are corrupt
     */
    final void loadFromProperties(String prefix, Map<String, String> source)
    throws ConfigurationException
    {
        controllerTypeCode = ConfigurationUtils.getInteger(
                source, prefix + ".controllerTypeCode");
        int numComponents = ConfigurationUtils.getInteger(
                source, prefix + ".numComponents");
        int numSubControllers = ConfigurationUtils.getInteger(
                source, prefix + ".numSubControllers");

        // Read all components
        for (int x=0; x<numComponents; x++)
        {
            componentConfigurations[x].loadFromProperties(
                    prefix + ".component" + x, source);
        }

        // Read all controllers
        for (int x=0; x<numSubControllers; x++)
        {
            subControllerConfigurations[x].loadFromProperties(
                    prefix + ".subController" + x, source);
        }
    }
}