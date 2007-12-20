package org.nicegamepads;

import java.util.HashMap;
import java.util.Map;

import net.java.games.input.Controller;

/**
 * Provides threadsafe access to configurations for all controllers.
 * 
 * @author Andrew Hayden
 */
public final class ConfigurationManager
{
    /**
     * Map of controller object keys to configuration values.
     * <p>
     * The controller objects do not persist across reconnects of the
     * physical device. 
     */
    private final static Map<Controller, ControllerConfiguration>
        configurationsByController =
            new HashMap<Controller, ControllerConfiguration>();

    /**
     * Lazy cache of immutable configuration objects for controllers.
     */
    private final static Map<Controller, ControllerConfiguration>
        cachedImmutableconfigurationsByController =
            new HashMap<Controller, ControllerConfiguration>();

    /**
     * Synchronization lock.
     */
    private final static Object lock = new Object();

    /**
     * Private constructor discourages unwanted instantiation.
     */
    private ConfigurationManager()
    {
        // Private constructor discourages unwanted instantiation.
    }

    /**
     * Sets the configuration for the specified controller.
     * <p>
     * If the specified configuration is <code>null</code>, then any
     * existing configuration bound to the specific controller is removed.
     * Otherwise, the configuration is immediately copied and the copy is
     * associated with the controller.
     * <p>
     * Note that as {@link ControllerConfiguration} is not threadsafe by
     * default (unless you're using an immutable object from
     * {@link ConfigurationUtils#immutableControllerConfiguration(ControllerConfiguration)}),
     * <strong>it is critically important that the caller not update the
     * configuration again until this call completes</strong>.  Failure
     * to comply may lead to a corrupted configuration being used, which
     * will cause undefined behavior.  Since modifying a mutable object
     * while you've given it to another class is generally frowned upon,
     * this isn't something most people should have to worry about.
     * 
     * @param controller the controller to associate the configuration with
     * @param configuration the configuration to associate with the controller;
     * if <code>null</code>, any existing configuration is cleared
     */
    public final static void setConfiguration(
            Controller controller, ControllerConfiguration configuration)
    {
        synchronized(lock)
        {
            if (configuration == null)
            {
                // Clear config.
                cachedImmutableconfigurationsByController.remove(controller);
                configurationsByController.remove(controller);
            }
            else
            {
                ControllerConfiguration cachedValue =
                    cachedImmutableconfigurationsByController.get(controller);
                if (cachedValue == null || !cachedValue.equals(configuration))
                {
                    // New config we have to use.
                    // Mark cache dirty.
                    cachedImmutableconfigurationsByController.remove(controller);
                    configurationsByController.put(controller,
                            new ControllerConfiguration(configuration));
                }
            }
        }
    }

    /**
     * Returns a mutable copy of the configuration for the specified
     * controller (only use if you actually require a mutable copy).
     * <p>
     * The returned copy is completely independent of the original.  It can
     * be safely modified and can be used in a subsequent call to
     * {@link #setConfiguration(Controller, ControllerConfiguration)}.
     * <p>
     * This method should <strong>only</strong> be used to obtain a
     * <strong>mutable copy</strong> of the configuration.  If you do
     * <em>not</em> need a mutable copy, you should call
     * {@link #getImmutableConfiguration(Controller)} instead, which
     * efficiently caches configurations until they are changed and
     * can be called as often as necessary.
     * 
     * @param controller the controller to get the configuration for
     * @return if the controller has previously been configured, an
     * independent and mutable copy of the configuration for that controller;
     * otherwise, <code>null</code>
     */
    public final static ControllerConfiguration getConfigurationCopy(
            Controller controller)
    {
        synchronized(lock)
        {
            ControllerConfiguration mutable =
                configurationsByController.get(controller);
            if (mutable != null)
            {
                return new ControllerConfiguration(mutable);
            }
            return null;
        }
    }

    /**
     * Returns a cached, immutable and independent copy of the configuration
     * for the specified controller.
     * <p>
     * Unlike {@link #getConfigurationCopy(Controller)}, this method will
     * only construct a new {@link ControllerConfiguration} if the cache
     * is dirty (that is, the configuration has been changed by a call
     * to {@link #setConfiguration(Controller, ControllerConfiguration)}
     * and the cache hasn't been updated yet).  This is much less expensive
     * than calling {@link #getConfigurationCopy(Controller)} in almost
     * all cases, and is the recommended way to ask for a configuration
     * so long as you do not need to alter it.
     * <p>
     * Any attempt to modify the immutable configuration will fail.  The
     * immutable configuration is also completely independent of the original
     * mutable source (it is not just a wrapper)
     * 
     * @param controller
     * @return
     */
    public final static ControllerConfiguration getImmutableConfiguration(
            Controller controller)
    {
        synchronized(lock)
        {
            ControllerConfiguration config =
                cachedImmutableconfigurationsByController.get(controller);
            if (config == null)
            {
                // Not cached.  Do lookup.
                ControllerConfiguration mutable =
                    configurationsByController.get(controller);
                if (mutable != null)
                {
                    // Found it.  Cache it and use as the return value.
                    config = ConfigurationUtils.immutableControllerConfiguration(mutable);
                    cachedImmutableconfigurationsByController.put(controller, config);
                }
            }
            return config;
        }
    }
}