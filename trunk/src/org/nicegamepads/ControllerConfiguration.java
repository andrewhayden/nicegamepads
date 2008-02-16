package org.nicegamepads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private LinkedHashMap<NiceControl, ControlConfiguration>
        controlConfigurations;

    /**
     * Cache of control locations in the subcontroller hierarchy.
     * <p>
     * This is used to speed binding and looking up configurations for
     * controls nested within a subcontroller.
     */
    private Map<NiceControl, ControllerConfiguration>
        cachedConfigurationsByControl =
            new HashMap<NiceControl, ControllerConfiguration>();

    /**
     * Fingerprint for the controller, can be used as a sanity check during
     * loading.
     */
    private int controllerFingerprint;

    /**
     * The controller that this configuration was generated for.
     * Note that this is <strong>NOT</strong> necessarily the controller that
     * the configuration is presently bound to (although this is probably
     * the case); it is just the controller that was used to generate the
     * data structures for this object.
     */
    private NiceController controller;

    /**
     * Creates a configuration comaptible with, but not specifically tied to,
     * the specified instance of controller.
     * <p>
     * The controller is used solely for determining the various attributes
     * that need to be persisted.  That is, it is used primarily for
     * examination of hardware.  A unique fingerprint for the controller is
     * inferred and kept as a sanity check for loading the configuration
     * in the future.
     * 
     * @param controller the controller to create a compatible configuration
     * for
     */
    ControllerConfiguration(NiceController controller)
    {
        // Copy will have blank cache of control locations by controller
        // config.  This is exactly what should happen, since we are creating
        // brand new config objects.

        // Generate fingerprint immediately.
        this.controller = controller;
        this.controllerFingerprint = controller.getFingerprint();

        // Fill in config info for controls
        List<NiceControl> controls = controller.getControls();
        int numControls = controls.size();
        controlConfigurations =
            new LinkedHashMap<NiceControl, ControlConfiguration>(numControls);
        for (NiceControl control : controls)
        {
            controlConfigurations.put(control, new ControlConfiguration());
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
        controller = source.controller;
        controllerFingerprint = source.controllerFingerprint;
        controlConfigurations =
            new LinkedHashMap<NiceControl, ControlConfiguration>(
                    source.controlConfigurations.size());

        // Copy will have blank cache of control locations by controller
        // config.  This is exactly what should happen, since we are creating
        // brand new configs everywhere.

        // Copy control configurations
        for (Map.Entry<NiceControl, ControlConfiguration> entry :
            source.controlConfigurations.entrySet())
        {
            ControlConfiguration sourceConfig = entry.getValue();
            if (sourceConfig != null)
            {
                controlConfigurations.put(entry.getKey(),
                        new ControlConfiguration(sourceConfig));
            }
            else
            {
                controlConfigurations.put(entry.getKey(), null);
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
        clone.controller = controller;
        clone.controllerFingerprint = controllerFingerprint;
        clone.controlConfigurations =
            new LinkedHashMap<NiceControl, ControlConfiguration>(
                    controlConfigurations.size());

        // Clone will have blank cache of control locations by controller
        // config.  This is exactly what should happen, since we are creating
        // brand new configs everywhere.

        // Copy control configurations
        for (Map.Entry<NiceControl, ControlConfiguration> entry :
            controlConfigurations.entrySet())
        {
            ControlConfiguration sourceConfig = entry.getValue();
            if (sourceConfig != null)
            {
                clone.controlConfigurations.put(entry.getKey(),
                        sourceConfig.clone());
            }
            else
            {
                clone.controlConfigurations.put(entry.getKey(), null);
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
    final NiceController getController()
    {
        return controller;
    }

    /**
     * Performs a breadth-first traversal of the entire controller hierarchy,
     * recording the control configurations for each controller before
     * descending into any subcontrollers.
     * 
     * @param configuration the configuration to start the traversal in
     * @return a breadth-first listing of all control configurations
     * found in or under this controller configuration
     */
    final static List<ControlConfiguration>
        enumerateAllControlConfigurations(
                ControllerConfiguration configuration)
    {
        // Do a traversal of the entire tree of configurations, breadth-first
        return new ArrayList<ControlConfiguration>(
                configuration.controlConfigurations.values());
    }

    /**
     * Saves this configuration to a mapping of (key,value) pairs
     * in an unambiguous manner suitable for user in a Java properties file.
     * <p>
     * The specified prefix, possibly with an added trailing "." character, is
     * prepended to the names of all properties written by this method.
     * <p>
     * This is a convenience method to call {@link #saveToMap(String, Map)}
     * with a <code>null</code> map, which causes that method to generate
     * and return a new map.
     * 
     * @param prefix the prefix to use for creating the property keys.
     * If <code>null</code> or an empty string, no prefix is set; otherwise,
     * the specified prefix is prepended to all values.  If the prefix does
     * not end with a ".", a "." is automatically inserted between the prefix
     * and the values.
     * @return a new {@link Map} containing this configuration's
     * (key,value) pairs
     */
    final Map<String, String> saveToMap(String prefix)
    {
        return saveToMap(prefix, null);
    }

    /**
     * Saves this configuration to a mapping of (key,value) pairs
     * in an unambiguous manner suitable for user in a Java properties file.
     * <p>
     * The specified prefix, possibly with an added trailing "." character, is
     * prepended to the names of all properties written by this method.
     * 
     * @param prefix the prefix to use for creating the property keys.
     * If <code>null</code> or an empty string, no prefix is set; otherwise,
     * the specified prefix is prepended to all values.  If the prefix does
     * not end with a ".", a "." is automatically inserted between the prefix
     * and the values.
     * @param destination optionally, a map into which the properties should
     * be written; if <code>null</code>, a new map is created and returned.
     * Any existing entries with the same names are overwritten.
     * @return if <code>destination</code> was specified, the reference to
     * that same object (which now contains this configuration's (key,value)
     * pairs); otherwise, a new {@link Map} containing this configuration's
     * (key,value) pairs
     */
    final Map<String, String> saveToMap(
            String prefix, Map<String,String> destination)
    {
        if (destination == null)
        {
            destination = new HashMap<String, String>();
        }

        // Check prefix and amend as necessary
        if (prefix != null && prefix.length() > 0)
        {
            if (!prefix.endsWith("."))
            {
                prefix = prefix + ".";
            }
        }
        else
        {
            prefix = "";
        }

        destination.put(prefix + "controllerFingerpring",
                Integer.toString(controllerFingerprint));
        destination.put(prefix + "numControls",
                Integer.toString(controlConfigurations.size()));

        // Write out all controls.
        int counter = 0;
        for (ControlConfiguration config : controlConfigurations.values())
        {
            config.saveToProperties(
                    prefix + "control" + counter, destination);
            counter++;
        }

        return destination;
    }

    /**
     * Returns the fingerprint from the specified mappings as if part of a
     * complete {@link #loadFromMap(String, Map)}, but does not
     * load the value into this configuration.
     * 
     * @param prefix the prefix, as in {@link #loadFromMap(String, Map)}
     * @param source the source to lookup the fingerprint in
     * @return the fingerprint
     * @throws ConfigurationException if the value isn't found in the
     * specified source
     */
    final static int readControllerFingerprintFromMap(
            String prefix, Map<String,String> source)
    throws ConfigurationException
    {
        // Check prefix and amend as necessary
        if (prefix != null && prefix.length() > 0)
        {
            if (!prefix.endsWith("."))
            {
                prefix = prefix + ".";
            }
        }
        else
        {
            prefix = "";
        }
        return ConfigurationUtils.getInteger(
                source, prefix + "controllerFingerprint");
    }

    /**
     * Loads a new configuration from Java properties.
     * <p>
     * All internal configuration objects are created anew, meaning that any
     * outstanding configuration objects are now orphaned (that is, the
     * configurations for each subcontroller and each control are created
     * fresh, and the references to the old ones are discarded).
     * 
     * @param prefix the prefix to use for retrieving the property keys,
     * which should be the same as when {@link #saveToMap(String, Map)}
     * If <code>null</code> or an empty string, no prefix is used; otherwise,
     * the specified prefix is assumed to be prepended to all values.
     * If the prefix does not end with a ".", a "." is automatically appended
     * for performing the lookups.
     * was originally called to save the configuration
     * @param source properties to read from
     * @throws ConfigurationException if any of the required keys or values
     * are missing or if any of the values are corrupt
     */
    final void loadFromMap(String prefix, Map<String, String> source)
    throws ConfigurationException
    {
        // Check prefix and amend as necessary
        if (prefix != null && prefix.length() > 0)
        {
            if (!prefix.endsWith("."))
            {
                prefix = prefix + ".";
            }
        }
        else
        {
            prefix = "";
        }

        controllerFingerprint = ConfigurationUtils.getInteger(
                source, prefix + "controllerFingerprint");
        int numControls = ConfigurationUtils.getInteger(
                source, prefix + "numControls");

        // Read all controls
        Iterator<NiceControl> controlIterator =
            controlConfigurations.keySet().iterator();
        for (int x=0; x<numControls; x++)
        {
            // Notice that order is preserved by the
            // "controlConfigurations" collection, so that our integer
            // mappings here will always be the same every single time
            // and will stay in-sync.
            ControlConfiguration config = new ControlConfiguration();
            config.loadFromProperties(
                    prefix + "control" + x, source);
            controlConfigurations.put(controlIterator.next(), config);
        }
    }

    /**
     * Sets the configuration for the specified control.
     * <p>
     * Note that this method will throw a {@link ConfigurationException} if
     * the caller attempts to bind a configuration to a nonexistent control.
     * Strictly speaking, no harm would be done by doing so - but if the caller
     * is trying to bind to a nonexistent control, then a serious logic
     * error has probably occurred on the calling side.
     * <p>
     * It is also possible to bind a configuration with a recursive search
     * through the child controls of any nested subcontrollers.  For more
     * information please see
     * {@link #setConfigurationDeep(Control, ControlConfiguration)}.
     * 
     * @param control the control to set the configuration for
     * @param configuration the configuration to set
     * @throws ConfigurationException if there is no such control in
     * this configuration
     * @see #setConfigurationDeep(Control, ControlConfiguration)
     */
    public void setConfiguration(
            NiceControl control, ControlConfiguration configuration)
    throws ConfigurationException
    {
        if (!controlConfigurations.containsKey(control))
        {
            throw new ConfigurationException(
                    "No such control in this configuration.");
        }
        controlConfigurations.put(control, configuration);
    }

    /**
     * Returns the configuration for the specified control.
     * <p>
     * It is also possible to find a configuration with a recursive search
     * through the child controls of any nested subcontrollers.  For more
     * information please see
     * {@link #getConfigurationDeep(Control)}.
     * <p>
     * Note that this method will throw a {@link ConfigurationException} if
     * the caller attempts to find a configuration to a nonexistent control.
     * Strictly speaking, no harm would be done by doing so - but if the caller
     * is trying to find to a nonexistent control, then a serious logic
     * error has probably occurred on the calling side.
     * 
     * @param control the control to retrieve the configuration for
     * @return the configuration for the specified control, if the
     * control exists in this configuration; otherwise, <code>null</code>
     */
    public final ControlConfiguration getConfiguration(NiceControl control)
    throws ConfigurationException
    {
        ControlConfiguration config = controlConfigurations.get(control);
        if (config == null)
        {
            throw new ConfigurationException(
                "No such control in this configuration.");
        }

        return config;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to mutate the control configurations.
     * 
     * @param controlConfigurations
     */
    final void setControlConfigurations(
            LinkedHashMap<NiceControl, ControlConfiguration> controlConfigurations)
    {
        this.controlConfigurations = controlConfigurations;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to mutate the controller fingerprint.
     * 
     * @param controllerTypeCode
     */
    final void setControllerFingerprint(int controllerTypeCode)
    {
        this.controllerFingerprint = controllerTypeCode;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to mutate the controller.
     * This method does not alter the fingerprint, which must be set
     * separately.
     * 
     * @param controller
     */
    final void setController(NiceController controller)
    {
        this.controller = controller;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to access the control configurations.
     * 
     * @return a reference to the live control configurations map itself
     */
    final LinkedHashMap<NiceControl, ControlConfiguration> getControlConfigurations()
    {
        return controlConfigurations;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to access the controller fingerprint.
     * 
     * @return the fingerprint of the controller
     */
    final int getControllerFingerprint()
    {
        return controllerFingerprint;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to access the cached configurations map.
     * 
     * @return the cached configurations map
     */
    final Map<NiceControl, ControllerConfiguration> getCachedConfigurationsByControl()
    {
        return cachedConfigurationsByControl;
    }

    /**
     * Not for public use.
     * <p>
     * Package-level access to access the cached configurations map.
     * 
     * @param cachedConfigurationsByControl new value for the map
     */
    final void setCachedConfigurationsByControl(
            Map<NiceControl, ControllerConfiguration> cachedConfigurationsByControl)
    {
        this.cachedConfigurationsByControl = cachedConfigurationsByControl;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        StringBuilder buffer = new StringBuilder();
        return toStringHelper(this, buffer, "");
    }

    /**
     * Recursively creates a string description of this object.
     * 
     * @param configuration the configuration to process recursively
     * @param buffer the buffer to append to
     * @param prefix prefix to place in front of each line
     * @return the string
     */
    private final static String toStringHelper(
            ControllerConfiguration configuration,
            StringBuilder buffer, String prefix)
    {
        buffer.append(prefix);
        buffer.append(ControllerConfiguration.class.getName());
        buffer.append(": [");
        buffer.append("controller=");
        buffer.append(configuration.controller);
        buffer.append(", controllerFingerprint=");
        buffer.append(configuration.controllerFingerprint);
        buffer.append("]\n");
        buffer.append(prefix);
        buffer.append("Control Configurations:\n");
        for (Map.Entry<NiceControl, ControlConfiguration> entry :
            configuration.controlConfigurations.entrySet())
        {
            buffer.append(prefix);
            buffer.append("    ");
            buffer.append(entry.getKey());
            buffer.append("=");
            buffer.append(entry.getValue());
            buffer.append("\n");
        }
        return buffer.toString();
    }
}