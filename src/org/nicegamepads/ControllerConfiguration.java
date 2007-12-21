package org.nicegamepads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
public class ControllerConfiguration implements Cloneable
{
    /**
     * Configurations for each control in the controller.
     * <p>
     * This is explicitly a linked hash map to make it known that insertion
     * order is preserved and that null values are allowed.
     */
    private LinkedHashMap<Component, ComponentConfiguration>
        componentConfigurations;

    /**
     * Configurations for each subcontroller that is a direct child of this
     * controller configuration.
     * <p>
     * This is explicitly a linked hash map to make it known that insertion
     * order is preserved and that null values are allowed.
     */
    private LinkedHashMap<Controller, ControllerConfiguration>
        subControllerConfigurations;

    /**
     * Cache of component locations in the subcontroller hierarchy.
     * <p>
     * This is used to speed binding and looking up configurations for
     * components nested within a subcontroller.
     */
    private Map<Component, ControllerConfiguration>
        cachedConfigurationsByComponent =
            new HashMap<Component, ControllerConfiguration>();

    /**
     * Type code for the controller, can be used as a sanity check during
     * loading.
     */
    private int controllerTypeCode;

    /**
     * The controller that this configuration was generated for.
     * Note that this is <strong>NOT</strong> necessarily the controller that
     * the configuration is presently bound to (although this is probably
     * the case); it is just the controller that was used to generate the
     * data structures for this object.
     */
    private Controller controller;

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
        // Copy will have blank cache of component locations by controller
        // config.  This is exactly what should happen, since we are creating
        // brand new config objects.

        // Generate type code immediately.
        this.controllerTypeCode = ControllerUtils.generateTypeCode(controller);

        // Fill in config info for components
        Component[] components = controller.getComponents();
        int numComponents = (components == null ? 0 : components.length);
        componentConfigurations =
            new LinkedHashMap<Component, ComponentConfiguration>(numComponents);
        for (int index=0; index<components.length; index++)
        {
            componentConfigurations.put(
                    components[index], new ComponentConfiguration());
        }

        // Fill in config info for nested controllers
        Controller[] subControllers = controller.getControllers();
        int numSubControllers = (subControllers == null ?
                0 : subControllers.length);
        subControllerConfigurations =
            new LinkedHashMap<Controller, ControllerConfiguration>(
                    numSubControllers);
        for (int index=0; index<subControllers.length; index++)
        {
            subControllerConfigurations.put(
                    subControllers[index], new ControllerConfiguration(
                            subControllers[index]));
        }
    }

    /**
     * Constructs an empty configuration.  Not for public use.
     */
    ControllerConfiguration()
    {
        // Do nothing.  This configuration will fail to load until a controller
        // is provided.
    }

    /**
     * Constructs a copy of the specified configuration.
     * <p>
     * This is a safe alternative to calling {@link #clone()}, which may
     * be overridden by subclasses.
     */
    ControllerConfiguration(ControllerConfiguration source)
    {
        controllerTypeCode = source.controllerTypeCode;
        componentConfigurations =
            new LinkedHashMap<Component, ComponentConfiguration>(
                    source.componentConfigurations.size());
        subControllerConfigurations =
            new LinkedHashMap<Controller, ControllerConfiguration>(
                    source.subControllerConfigurations.size());

        // Copy will have blank cache of component locations by controller
        // config.  This is exactly what should happen, since we are creating
        // brand new configs everywhere.

        // Copy component configurations
        for (Map.Entry<Component, ComponentConfiguration> entry :
            source.componentConfigurations.entrySet())
        {
            ComponentConfiguration sourceConfig = entry.getValue();
            if (sourceConfig != null)
            {
                componentConfigurations.put(entry.getKey(),
                        new ComponentConfiguration(sourceConfig));
            }
            else
            {
                componentConfigurations.put(entry.getKey(), null);
            }
        }

        // Recursively copy subcontroller configurations
        for (Map.Entry<Controller, ControllerConfiguration> entry :
            source.subControllerConfigurations.entrySet())
        {
            ControllerConfiguration sourceConfig = entry.getValue();
            if (sourceConfig != null)
            {
                subControllerConfigurations.put(entry.getKey(),
                        new ControllerConfiguration(sourceConfig));
            }
            else
            {
                subControllerConfigurations.put(entry.getKey(), null);
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    protected ControllerConfiguration clone()
    throws CloneNotSupportedException
    {
        super.clone();

        // Create new blank configuration object
        ControllerConfiguration clone =
            new ControllerConfiguration();
        clone.controllerTypeCode = controllerTypeCode;
        clone.componentConfigurations =
            new LinkedHashMap<Component, ComponentConfiguration>(
                    componentConfigurations.size());
        clone.subControllerConfigurations =
            new LinkedHashMap<Controller, ControllerConfiguration>(
                    subControllerConfigurations.size());

        // Clone will have blank cache of component locations by controller
        // config.  This is exactly what should happen, since we are creating
        // brand new configs everywhere.

        // Copy component configurations
        for (Map.Entry<Component, ComponentConfiguration> entry :
            componentConfigurations.entrySet())
        {
            ComponentConfiguration sourceConfig = entry.getValue();
            if (sourceConfig != null)
            {
                clone.componentConfigurations.put(entry.getKey(),
                        sourceConfig.clone());
            }
            else
            {
                clone.componentConfigurations.put(entry.getKey(), null);
            }
        }

        // Recursively copy subcontroller configurations
        for (Map.Entry<Controller, ControllerConfiguration> entry :
            subControllerConfigurations.entrySet())
        {
            ControllerConfiguration sourceConfig = entry.getValue();
            if (sourceConfig != null)
            {
                clone.subControllerConfigurations.put(entry.getKey(),
                        sourceConfig.clone());
            }
            else
            {
                clone.subControllerConfigurations.put(entry.getKey(), null);
            }
        }

        return clone;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to the controller that was used to generate
     * this object.  Note that this has no meaning with regards to what
     * controller this configuration is bound to (indeed, the controller
     * may not even be attached to the system any more); it is purely for
     * the purpose of cloning and introspection.
     * 
     * @return the controller that was used to generate this configuration
     */
    final Controller getController()
    {
        return controller;
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
        for (ComponentConfiguration config : target.componentConfigurations.values())
        {
            allConfigs.add(config);
        }

        for (ControllerConfiguration config : target.subControllerConfigurations.values())
        {
            enumerationHelper(config, allConfigs);
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
                Integer.toString(componentConfigurations.size()));
        destination.put(prefix + ".numSubControllers",
                Integer.toString(subControllerConfigurations.size()));

        // Write out all components.
        int counter = 0;
        for (ComponentConfiguration config : componentConfigurations.values())
        {
            config.saveToProperties(
                    prefix + ".component" + counter, destination);
            counter++;
        }

        counter = 0;
        for (ControllerConfiguration config : subControllerConfigurations.values())
        {
            config.saveToProperties(
                    prefix + ".subController" + counter, destination);
            counter++;
        }

        return destination;
    }

    /**
     * Loads a new configuration from Java properties.
     * <p>
     * All internal configuration objects are created anew, meaning that any
     * outstanding configuration objects are now orphaned (that is, the
     * configurations for each subcontroller and each component are created
     * fresh, and the references to the old ones are discarded).
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
        Iterator<Component> componentIterator =
            componentConfigurations.keySet().iterator();
        for (int x=0; x<numComponents; x++)
        {
            // Notice that order is preserved by the
            // "componentConfigurations" collection, so that our integer
            // mappings here will always be the same every single time
            // and will stay in-sync.
            ComponentConfiguration config = new ComponentConfiguration();
            config.loadFromProperties(
                    prefix + ".component" + x, source);
            componentConfigurations.put(componentIterator.next(), config);
        }

        // Read all controllers
        Iterator<Controller> controllerIterator =
            subControllerConfigurations.keySet().iterator();
        for (int x=0; x<numSubControllers; x++)
        {
            // Notice that order is preserved by the
            // "controllerConfigurations" collection, so that our integer
            // mappings here will always be the same every single time
            // and will stay in-sync.
            Controller controller = controllerIterator.next();
            ControllerConfiguration config =
                new ControllerConfiguration(controller);
            config.loadFromProperties(
                    prefix + ".subController" + x, source);
            subControllerConfigurations.put(controller, config);
        }
    }

    /**
     * Sets the configuration for the specified component.
     * <p>
     * Note that this method will throw a {@link ConfigurationException} if
     * the caller attempts to bind a configuration to a nonexistent component.
     * Strictly speaking, no harm would be done by doing so - but if the caller
     * is trying to bind to a nonexistent component, then a serious logic
     * error has probably occurred on the calling side.
     * <p>
     * It is also possible to bind a configuration with a recursive search
     * through the child components of any nested subcontrollers.  For more
     * information please see
     * {@link #setConfigurationDeep(Component, ComponentConfiguration)}.
     * 
     * @param component the component to set the configuration for
     * @param configuration the configuration to set
     * @throws ConfigurationException if there is no such component in
     * this configuration
     * @see #setConfigurationDeep(Component, ComponentConfiguration)
     */
    public void setConfiguration(
            Component component, ComponentConfiguration configuration)
    throws ConfigurationException
    {
        if (!componentConfigurations.containsKey(component))
        {
            throw new ConfigurationException(
                    "No such component in this configuration.");
        }
        componentConfigurations.put(component, configuration);
    }

    /**
     * Returns the configuration for the specified component.
     * <p>
     * It is also possible to find a configuration with a recursive search
     * through the child components of any nested subcontrollers.  For more
     * information please see
     * {@link #getConfigurationDeep(Component)}.
     * <p>
     * Note that this method will throw a {@link ConfigurationException} if
     * the caller attempts to find a configuration to a nonexistent component.
     * Strictly speaking, no harm would be done by doing so - but if the caller
     * is trying to find to a nonexistent component, then a serious logic
     * error has probably occurred on the calling side.
     * 
     * @param component the component to retrieve the configuration for
     * @return the configuration for the specified component, if the
     * component exists in this configuration; otherwise, <code>null</code>
     */
    public final ComponentConfiguration getConfiguration(Component component)
    throws ConfigurationException
    {
        ComponentConfiguration config = componentConfigurations.get(component);
        if (config == null)
        {
            throw new ConfigurationException(
                "No such component in this configuration.");
        }

        return config;
    }

    /**
     * Returns the configuration for the specified component, searching
     * any and all subcontroller configurations if necessary.
     * <p>
     * Since components cannot be added to or removed from a configuration
     * at runtime, recursive search results are cached for efficiency.
     * Future lookups of the same component in the same configuration object
     * will generally be very fast (no attempt is made to speed up lookups
     * for non-existent components, however).
     * <p>
     * Note that this method will throw a {@link ConfigurationException} if
     * the caller attempts to find a configuration to a nonexistent component.
     * Strictly speaking, no harm would be done by doing so - but if the caller
     * is trying to find to a nonexistent component, then a serious logic
     * error has probably occurred on the calling side.
     * 
     * @param component the component to retrieve the configuration for
     * @return the configuration for the specified component, if the
     * component exists in this configuration; otherwise, <code>null</code>
     */
    public final ComponentConfiguration getConfigurationDeep(
            Component component)
    throws ConfigurationException
    {
        ControllerConfiguration source =
            cachedConfigurationsByComponent.get(component);
        if (source == null)
        {
            source = componentSearchHelper(this, component);
            if (source == null)
            {
                throw new ConfigurationException(
                    "No such component in this configuration.");
            }

            // If we get this far, we found something.  Cache it for
            // future speed.
            cachedConfigurationsByComponent.put(component, source);
        }
        return source.getConfiguration(component);
    }

    /**
     * Sets the configuration for the specified configuration, searching
     * subcontrollers as necessary until the specified component is located.
     * <p>
     * Unlike {@link #setConfiguration(Component, ComponentConfiguration)},
     * this method performs a deep search into any and all subcontrollers
     * (if necessary) trying to locate the component.  This is convenient
     * when the caller does not care where the component resides, and is
     * only concerned with the "flattened" set of all components in the
     * controller (as might be found using the utility method
     * {@link ControllerUtils#getComponents(Controller, boolean)}).
     * <p>
     * Note that this method will throw a {@link ConfigurationException} if
     * the caller attempts to bind a configuration to a nonexistent component.
     * Strictly speaking, no harm would be done by doing so - but if the caller
     * is trying to bind to a nonexistent component, then a serious logic
     * error has probably occurred on the calling side.
     * <p>
     * Since components cannot be added to or removed from a configuration
     * at runtime, recursive search results are cached for efficiency.
     * Future updates to the same component in the same configuration object
     * will generally be very fast (no attempt is made to speed up lookups
     * for non-existent components, however).
     * 
     * @param component the top-level component to start searching in
     * @param configuration the configuration to set
     * @throws ConfigurationException if there is no such component in
     * this configuration or any of its subcontroller configurations
     */
    public final void setConfigurationDeep(
            Component component, ComponentConfiguration configuration)
    throws ConfigurationException
    {
        ControllerConfiguration target =
            cachedConfigurationsByComponent.get(component);
        if (target == null)
        {
            target = componentSearchHelper(this, component);
            if (target == null)
            {
                throw new ConfigurationException(
                    "No such component in this configuration.");
            }
    
            // If we get this far, we found something.  Cache it for
            // future speed.
            cachedConfigurationsByComponent.put(component, target);
        }

        target.setConfiguration(component, configuration);
    }

    /**
     * Recursively searches the controller hierarchy for the specified
     * target component.
     * 
     * @param configuration the configuration to begin searching in
     * @param target the component to locate
     * @return if the component is found, the controller configuration that
     * contains the component; otherwise, <code>null</code>
     */
    private final ControllerConfiguration componentSearchHelper(
            ControllerConfiguration configuration, Component target)
    {
        if (configuration.componentConfigurations.containsKey(target))
        {
            return configuration;
        }

        ControllerConfiguration result = null;
        for (ControllerConfiguration subConfig :
            subControllerConfigurations.values())
        {
            result = componentSearchHelper(subConfig, target);
            if (result != null)
            {
                return result;
            }
        }

        return null;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to mutate the component configurations.
     * 
     * @param componentConfigurations
     */
    final void setComponentConfigurations(
            LinkedHashMap<Component, ComponentConfiguration> componentConfigurations)
    {
        this.componentConfigurations = componentConfigurations;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to mutate the controller configurations.
     * 
     * @param subControllerConfigurations
     */
    final void setSubControllerConfigurations(
            LinkedHashMap<Controller, ControllerConfiguration> subControllerConfigurations)
    {
        this.subControllerConfigurations = subControllerConfigurations;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to mutate the controller type code.
     * 
     * @param controllerTypeCode
     */
    final void setControllerTypeCode(int controllerTypeCode)
    {
        this.controllerTypeCode = controllerTypeCode;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to mutate the controller.
     * This method does not alter the type code, which must be set
     * separately.
     * 
     * @param controller
     */
    final void setController(Controller controller)
    {
        this.controller = controller;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to access the component configurations.
     * 
     * @return a reference to the live component configurations map itself
     */
    final LinkedHashMap<Component, ComponentConfiguration> getComponentConfigurations()
    {
        return componentConfigurations;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to access the controller configurations.
     * 
     * @return a reference to the live controller configurations map itself
     */
    final LinkedHashMap<Controller, ControllerConfiguration>
    getSubControllerConfigurations()
    {
        return subControllerConfigurations;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to access the controller type code.
     * 
     * @return the type code of the controller
     */
    final int getControllerTypeCode()
    {
        return controllerTypeCode;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to access the cached configurations map.
     * 
     * @return the cached configurations map
     */
    final Map<Component, ControllerConfiguration> getCachedConfigurationsByComponent()
    {
        return cachedConfigurationsByComponent;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to access the cached configurations map.
     * 
     * @param cachedConfigurationsByComponent new value for the map
     */
    final void setCachedConfigurationsByComponent(
            Map<Component, ControllerConfiguration> cachedConfigurationsByComponent)
    {
        this.cachedConfigurationsByComponent = cachedConfigurationsByComponent;
    }
}