package org.nicegamepads;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents the configuration for a controller.
 * <p>
 * This class is threadsafe.
 * 
 * @author Andrew Hayden
 */
public class ControllerConfiguration
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
     * The controller that this configuration was generated for.
     */
    private final NiceController controller;

    /**
     * Synchronizes all modification.
     */
    private final ReentrantLock modificationLock = new ReentrantLock(false);

    /**
     * Modification counter.
     */
    private volatile int modificationCount = Integer.MIN_VALUE;

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
        this.controller = controller;

        // Fill in config info for controls
        List<NiceControl> controls = controller.getControls();
        int numControls = controls.size();
        controlConfigurations =
            new LinkedHashMap<NiceControl, ControlConfiguration>(numControls);
        for (NiceControl control : controls)
        {
            controlConfigurations.put(control,
                    new ControlConfiguration(this, control));
        }
    }

    /**
     * Constructs a copy of the specified configuration.
     * <p>
     * This is a safe alternative to calling {@link #clone()}, which may
     * be overridden by subclasses.
     */
    ControllerConfiguration(ControllerConfiguration source)
    {
        source.lockConfiguration();
        controller = source.controller;
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
        source.unlockConfiguration();
    }

    /**
     * Loads all the settings from the specified configuration into this
     * configuration.  This method can be used to copy settings between
     * different controllers so long as their fingerprints are identical.
     * 
     * @param other the source object to copy settings from
     * @throws ConfigurationException if the specified other configuration
     * has a different fingerprint than this controller
     */
    public void loadFrom(ControllerConfiguration other)
    {
        if (other == this)
        {
            return;
        }
        else if (other.controller != controller)
        {
            if (other.controller.getFingerprint()
                    != controller.getFingerprint())
            {
                throw new ConfigurationException(
                        "Specified source configuration has fingerprint "
                        + other.controller.getFingerprint()
                        + ", which does not match the destination fingerprint "
                        + controller.getFingerprint());
            }

            // Need to translate into a map and load the map, because we
            // don't have references to the same objects
            Map<String, String> otherConfigAsMap = other.saveToMap("internal");
            loadFromMap("internal", otherConfigAsMap);
        }
        else
        {
            // Same underlying objects; we can do this more efficiently.
            // Must construct a copy to ensure thread safety while avoiding
            // deadlock; otherwise we'd have to acquire both the sources' lock
            // and our own to do this safely.
            ControllerConfiguration source =
                new ControllerConfiguration(other);
            lockConfiguration();
            for (ControlConfiguration controlConfig :
                controlConfigurations.values())
            {
                controlConfig.loadFrom(
                        source.getConfiguration(controlConfig.control));
            }
            unlockConfiguration(true);
        }
    }

    final void lockConfiguration()
    {
        modificationLock.lock();
    }

    final void unlockConfiguration()
    {
        modificationLock.unlock();
    }

    /**
     * Unlocks the configuration and records whether or not the locking
     * operation resulted in a change or not.
     * 
     * @param markDirty if <code>true</code>, indicates that the configuration
     * has changed and should be marked as dirty; otherwise, indicates
     * that the locking operation completed without making any changes
     */
    final void unlockConfiguration(boolean markDirty)
    {
        if (markDirty)
        {
            // Wraps around at Integer.MAX_VALUE, back to Integer.MIN_VALUE.
            modificationCount++;
        }
        modificationLock.unlock();
    }

    /**
     * Returns the modification count.  This is an indication of how many
     * times the configuration has been modified, and can be used to cache
     * changes intelligently.  The count is updated every time a change is
     * made, and wraps around when it reachs {@link Integer#MAX_VALUE}.
     * 
     * @return the modification count
     */
    final int getModificationCount()
    {
        return modificationCount;
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

        lockConfiguration();
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
        unlockConfiguration();

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
     * configurations for each control are created fresh, and the references
     * to the old ones are discarded).
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

        int numControls = ConfigurationUtils.getInteger(
                source, prefix + "numControls");

        lockConfiguration();
        // Read all controls
        Iterator<NiceControl> controlIterator =
            controlConfigurations.keySet().iterator();
        for (int x=0; x<numControls; x++)
        {
            NiceControl control = controlIterator.next();
            // Notice that order is preserved by the
            // "controlConfigurations" collection, so that our integer
            // mappings here will always be the same every single time
            // and will stay in-sync.
            ControlConfiguration config =
                new ControlConfiguration(this, control);
            config.loadFromProperties(
                    prefix + "control" + x, source);
            controlConfigurations.put(control, config);
        }
        unlockConfiguration(true);
    }

    /**
     * Returns the configuration for the specified control.
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
        lockConfiguration();
        ControlConfiguration config = controlConfigurations.get(control);
        unlockConfiguration();
        if (config == null)
        {
            throw new ConfigurationException(
                "No such control in this configuration.");
        }
        return config;
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
        buffer.append(": ");
        buffer.append("controller=");
        buffer.append(configuration.controller);
        buffer.append("\n");
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

    /**
     * Returns the controller that this configuration was created for.
     * 
     * @return the controller that this configuration was created for.
     */
    public final NiceController getController()
    {
        return controller;
    }
}