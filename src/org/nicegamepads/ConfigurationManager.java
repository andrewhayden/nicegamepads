package org.nicegamepads;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides threadsafe access to configurations for all controllers.
 * 
 * @author Andrew Hayden
 */
public final class ConfigurationManager
{
    /**
     * Standard prefix used for saving configurations.
     */
    private static final String STANDARD_PREFIX =
        "org.nicegamepads.ConfigurationManager";

    /**
     * Map of controller object keys to configuration values.
     * <p>
     * The controller objects do not persist across reconnects of the
     * physical device. 
     */
    private final static Map<NiceController, ControllerConfiguration>
        configurationsByController =
            new HashMap<NiceController, ControllerConfiguration>();

    /**
     * Lazy cache of immutable configuration objects for controllers.
     */
    private final static Map<NiceController, ControllerConfiguration>
        cachedImmutableconfigurationsByController =
            new HashMap<NiceController, ControllerConfiguration>();

    /**
     * Synchronization lock.
     */
    private final static Object configurationLock = new Object();

    /**
     * Default location for storing configuration files.
     * <p>
     * The default value is ".controller-configs", which is interpreted as a
     * relative path to the current working directory.
     */
    public final static String DEFAULT_CONFIG_PATH = ".controller-configs/";

    /**
     * Key under which the {@link #DEFAULT_CONFIG_PATH} may be overridden by
     * clients.  Setting a system property with this name to a viable
     * path will cause the system to save and load configurations from that
     * path instead of {@link #DEFAULT_CONFIG_PATH}.
     */
    public final static String CONFIG_PATH_PROPERTY =
        "org.nicegamepads.ConfigurationManager.configPath";

    /**
     * Major version of this class.  Configurations saved by different major
     * versions are not guaranteed to be compatible.
     * <p>
     * This version has nothing to do with the version of the library as a
     * whole.  It is maintained separately.
     * 
     * @see #MINOR_VERSION
     */
    public final static int MAJOR_VERSION = 0;

    /**
     * Minor version of this class.  Configurations saved by different minor
     * versions (with the same major version) are guaranteed to be compatible.
     * <p>
     * This version has nothing to do with the version of the library as a
     * whole.  It is maintained separately.
     * 
     * @see #MAJOR_VERSION
     */
    public final static int MINOR_VERSION = 1;

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
            NiceController controller, ControllerConfiguration configuration)
    {
        synchronized(configurationLock)
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
            NiceController controller)
    {
        synchronized(configurationLock)
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
            NiceController controller)
    {
        synchronized(configurationLock)
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
                    config = ConfigurationUtils
                        .immutableControllerConfiguration(mutable);
                    cachedImmutableconfigurationsByController.put(
                            controller, config);
                }
            }
            return config;
        }
    }

    /**
     * Makes a comment for output.
     * 
     * @return a comment for output
     */
    private final static String getDefaultComment()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("This file was automatically generated by ");
        buffer.append(ConfigurationManager.class.toString());
        buffer.append(" v");
        buffer.append(MAJOR_VERSION);
        buffer.append(".");
        buffer.append(MINOR_VERSION);
        buffer.append(" on ");
        SimpleDateFormat dateFormat =
            new SimpleDateFormat("EEEE, dd MMMM 'at' hh:mm:ss a, z");
        buffer.append(dateFormat.format(new Date()));
        buffer.append(".  Do not edit by hand.");
        return buffer.toString();
    }

    /**
     * Returns the path that should be used for configuration saving and
     * loading.  All paths are normalized to UNIX form, and all paths are
     * suffixed with a trailing "/" if it is not already present.
     * 
     * @return the path that should be used for configuration saving and
     * loading.
     */
    private final static String getConfigPath()
    {
        String path = System.getProperty(CONFIG_PATH_PROPERTY);
        if (path != null)
        {
            path = path.replaceAll("\\", "/");
            if (!path.endsWith("/"))
            {
                path += "/";
            }
        }
        else
        {
            path = DEFAULT_CONFIG_PATH;
        }

        return path;
    }


    /**
     * Derives the configuration file path, optionally using a namespace
     * qualifier.
     * 
     * @param controller the controller whose type code should be used to
     * generate the path
     * @param namespace (optional) the namespace that should be used to
     * generate the path
     * @return a path unique to the controller's type code within the given
     * namespace
     */
    private final static String inferConfigFilePath(
            NiceController controller, String namespace)
    {
        if (namespace != null && namespace.length() > 0)
        {
            return getConfigPath() + namespace + "_"
                + controller.getFingerprint();
        }

        return getConfigPath() + controller.getFingerprint();
    }

    /**
     * Saves the specified configuration to a file, deriving a unique name
     * based on a heuristic analysis of the controller's logical layout.
     * <p>
     * The file is saved in the directory identified by the system property
     * having the name {@link #CONFIG_PATH_PROPERTY}.  If there is no
     * system property with this name, then {@link #DEFAULT_CONFIG_PATH} is
     * used instead.
     * <p>
     * The name of the file is based on the type code generated by
     * {@link ControllerUtils#generateFingerprint(Controller)}, and is extremely
     * likely to be unique for the specific type of controller associated
     * with the configuration.
     * <p>
     * This method is particularly useful for persisting a configuration for
     * a specific <em>type</em> of controller as opposed to a specific
     * <em>use</em> of such a controller.  For example, you might wish to
     * store a single configuration for all XBox-360 USB controllers,
     * perhaps for a single-user application that just needs the controller
     * to work when installed.
     * 
     * @param configuration the configuration to save
     * @return a {@link File} object denoting the file into which the
     * information was saved
     * @see #loadConfigurationByType(Controller)
     * @throws IOException if there is a problem while writing the file
     */
    public final static File saveConfigurationByType(
            ControllerConfiguration configuration)
    throws IOException
    {
        return saveConfigurationByType(configuration, null);
    }

    /**
     * Saves the specified configuration to a file, using the specified
     * namespace and a unique identifier generated by a
     * heuristic analysis of the controller's logical layout.
     * <p>
     * The file is saved in the directory identified by the system property
     * having the name {@link #CONFIG_PATH_PROPERTY}.  If there is no
     * system property with this name, then {@link #DEFAULT_CONFIG_PATH} is
     * used instead.
     * <p>
     * This method is particularly useful for persisting a configuration for
     * a specific <em>use</em> of a specific type of controller.  For example,
     * you might wish to store multiple configurations for
     * potentially-identical controller <em>types</em>, but differentiate
     * between even identical types by the <em>purpose</em> or <em>use</em>
     * of the device - as is common in user-specific environments and
     * multi-player games (where multiple users may all have access to the
     * same physical hardware, but may wish to use that hardware in different
     * ways).
     * <p>
     * Note that the namespace specified does not <em>fully constitute</em> the
     * name of the file generated.  For uniqueness purposes, the name is
     * combined with an identifier that constitutes a heuristic "fingerprint"
     * of the logical device.  In this way, you can store the same
     * <em>named</em> configurations for many devices - for example, you
     * may save multiple configurations under the name "Player1" for all
     * of the controllers that "player 1" of a multi-player game has
     * configured for use.  Since the name is combined with the fingerprint
     * of the device, player 1 can have many different controller
     * configurations available simultaneously.
     * 
     * @param configuration the configuration to save
     * @param namespace the name to use in the configuration file
     * name; combined with the type code automatically
     * @return a {@link File} object denoting the file into which the
     * information was saved
     * @see #loadConfigurationByType(Controller, String)
     * @throws IOException if there is a problem while writing the file
     */
    public final static File saveConfigurationByType(
            ControllerConfiguration configuration, String namespace)
    throws IOException
    {
        File destinationFile = new File(
                inferConfigFilePath(
                        configuration.getController(), namespace));
        saveConfiguration(configuration, destinationFile);
        return destinationFile;
    }

    /**
     * Saves the specified configuration to the specified file.
     * <p>
     * Unlike {@link #saveConfigurationByType(ControllerConfiguration, String)}
     * and {@link #saveConfigurationByType(ControllerConfiguration)}, this
     * method saves the configuration to the exact file specified.
     * <strong>If the file already exists, it will be overwritten.</strong>
     * <p>
     * If the directory in which the file is to reside does not exist,
     * it is created.
     * 
     * @param configuration the configuration to save
     * @param destinationFile the file to save the configuration to
     * @see #loadConfiguration(Controller, File)
     * @throws IOException if there is a problem while writing the file
     */
    public final static void saveConfiguration(
            ControllerConfiguration configuration, File destinationFile)
    throws IOException
    {
        File parentDirectory = destinationFile.getParentFile();
        if (parentDirectory != null && !parentDirectory.exists())
        {
            // Make directories first if necessary
            parentDirectory.mkdirs();
        }

        Map<String,String> asMap = configuration.saveToMap(
                STANDARD_PREFIX, null);
        asMap.put(STANDARD_PREFIX + ".majorVersion",
                Integer.toString(MAJOR_VERSION));
        asMap.put(STANDARD_PREFIX + ".minorVersion",
                Integer.toString(MINOR_VERSION));

        Properties asProperties = new Properties();
        asProperties.putAll(asMap);
        FileOutputStream fileOutput = new FileOutputStream(
                destinationFile, false);
        asProperties.storeToXML(fileOutput, getDefaultComment());
    }

    /**
     * Loads a configuration for the specified controller,
     * deriving a unique name based on a heuristic analysis of the
     * controller's logical layout.
     * <p>
     * The file is loaded from the directory identified by the system property
     * having the name {@link #CONFIG_PATH_PROPERTY}.  If there is no
     * system property with this name, then {@link #DEFAULT_CONFIG_PATH} is
     * used instead.
     * <p>
     * Generally, this method is called by applications that have elected
     * to use
     * {@link #saveConfigurationByType(ControllerConfiguration)}
     * to manage their configurations.
     * 
     * @param controller the controller to load the configuration for
     * @return the configuration loaded
     * @see #saveConfigurationByType(ControllerConfiguration)
     * @throws IOException if there is a problem reading from the file
     * @throws ConfigurationException if the configuration file is corrupt
     * or cannot be interpreted due to a version mismatch (i.e., trying to
     * read a configuration generated by a newer major version of this library)
     */
    public final static ControllerConfiguration loadConfigurationByType(
            NiceController controller)
    throws IOException, ConfigurationException
    {
        return loadConfigurationByType(controller, null);
    }

    /**
     * Loads a configuration for the specified controller from the specified
     * namespace.
     * <p>
     * The file is loaded from the directory identified by the system property
     * having the name {@link #CONFIG_PATH_PROPERTY}.  If there is no
     * system property with this name, then {@link #DEFAULT_CONFIG_PATH} is
     * used instead.
     * <p>
     * Generally, this method is called by applications that have elected
     * to use
     * {@link #saveConfigurationByType(ControllerConfiguration, String)}
     * to manage their configurations.
     * 
     * @param controller the controller to load the configuration for
     * @param namespace the name to use in the configuration file
     * name; combined with the controller type code automatically
     * @return the configuration loaded
     * @see #saveConfigurationByType(ControllerConfiguration, String)
     * @throws IOException if there is a problem reading from the file
     * @throws ConfigurationException if the configuration file is corrupt
     * or cannot be interpreted due to a version mismatch (i.e., trying to
     * read a configuration generated by a newer major version of this library)
     */
    public final static ControllerConfiguration loadConfigurationByType(
            NiceController controller, String namespace)
    throws IOException, ConfigurationException
    {
        File sourceFile = new File(
                inferConfigFilePath(controller, namespace));
        return loadConfiguration(controller, sourceFile);
    }

    /**
     * Loads a configuration for the specified controller from the specified
     * file.
     * <p>
     * This is generally only useful for applications that wish to directly
     * manager their configurations, and are using
     * {@link #saveConfiguration(ControllerConfiguration, File)} to persist
     * configuration data.
     * 
     * @param controller the controller to load the configuration for
     * @param sourceFile the file to read from, which must be a file
     * previously output by
     * {@link #saveConfiguration(ControllerConfiguration, File)} or one of
     * its variants.
     * @return the configuration loaded
     * @throws IOException if there is a problem reading from the file
     * @throws ConfigurationException if the configuration file is corrupt
     * or cannot be interpreted due to a version mismatch (i.e., trying to
     * read a configuration generated by a newer major version of this library)
     */
    public final static ControllerConfiguration loadConfiguration(
            NiceController controller, File sourceFile)
    throws IOException, ConfigurationException
    {
        Properties asProperties = new Properties();
        FileInputStream fileInput = new FileInputStream(sourceFile);
        asProperties.loadFromXML(fileInput);
        Map<String,String> asMap = new HashMap<String, String>();
        try
        {
            for (Map.Entry<Object, Object> property : asProperties.entrySet())
            {
                // java.util.Properties is a Map<Object,Object>, but we know
                // for certain that every entry we loaded came from a file
                // we own the format of.  Every entry will be a <String,String>
                // tuple.  Therefore, this is safe.
                asMap.put((String) property.getKey(),
                        (String) property.getValue());
            }
        }
        catch(ClassCastException e)
        {
            throw new ConfigurationException(
                    "Configuration file is corrupt: " + sourceFile, e);
        }

        // Confirm major version compatibility...
        String majorVersionString = asMap.get(
                STANDARD_PREFIX + ".majorVersion");
        if (majorVersionString == null)
        {
            throw new ConfigurationException(
                    "No major version listed in configuration file: "
                    + sourceFile);
        }

        String minorVersionString = asMap.get(
                STANDARD_PREFIX + ".minorVersion");
        if (majorVersionString == null)
        {
            throw new ConfigurationException(
                    "No minor version listed in configuration file: "
                    + sourceFile);
        }

        final int fileMajorVersion;
        try
        {
            fileMajorVersion = Integer.parseInt(majorVersionString, 10);
        }
        catch(NumberFormatException e)
        {
            throw new ConfigurationException(
                    "Major version listed in configuration file '"
                    + sourceFile + "' is corrupt: " + majorVersionString, e);
        }

        final int fileMinorVersion;
        try
        {
            fileMinorVersion = Integer.parseInt(minorVersionString, 10);
        }
        catch(NumberFormatException e)
        {
            throw new ConfigurationException(
                    "Minor version listed in configuration file '"
                    + sourceFile + "' is corrupt: " + minorVersionString, e);
        }

        if (fileMajorVersion > MAJOR_VERSION)
        {
            throw new ConfigurationException(
                    "Specified configuration file '"
                    + sourceFile + "' was output in format "
                    + fileMajorVersion + "." + fileMinorVersion
                    + ", but this runtime is using an incompatible (older) "
                    + "format " + MAJOR_VERSION + "." + MINOR_VERSION
                    + ".  Unable to load configuration.");
        }

        // Check type codes
        final int loadedFingerprint;
        try
        {
            loadedFingerprint =
                ControllerConfiguration.readControllerTypeCodeFromMap(
                        STANDARD_PREFIX, asMap);
        }
        catch(ConfigurationException e)
        {
            throw new ConfigurationException(
                    "Specified configuration file '" + sourceFile
                    + "' doesn't declare a controller fingerprint.");
        }

        final int fingerprint = controller.getFingerprint();

        if (fingerprint != loadedFingerprint)
        {
            throw new ConfigurationException(
                    "Controller fingerprint mismatch: actual != loaded: "
                    + fingerprint + " != " + loadedFingerprint
                    + ".  The configuration file '" + sourceFile
                    + "' was generated against a different type of "
                    + "controller and cannot be loaded.");
        }

        // Got this far?  Everything looks good.  Go for it!
        ControllerConfiguration configuration =
            new ControllerConfiguration(controller);
        configuration.loadFromMap(STANDARD_PREFIX, asMap);
        return configuration;
    }
}