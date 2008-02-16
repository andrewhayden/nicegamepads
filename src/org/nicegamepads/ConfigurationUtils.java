package org.nicegamepads;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ConfigurationUtils
{
    /**
     * Private constructor discourages unwanted instantiation.
     */
    private ConfigurationUtils()
    {
        // Private constructor discourages unwanted instantiation.
    }

    /**
     * Makes an independent, immutable copy of the specified configuration.
     * <p>
     * The returned object is completely independent of the original source.
     * <p>
     * This method is threadsafe but the source object being copied may not
     * be.  It is up to the caller to ensure that the configuration being
     * copied cannot be modified during the copying process.
     * 
     * @param source the configuration to copy
     * @return an independent, read-only copy of the configuration
     */
    final static ControlConfiguration immutableComponentConfiguration(
            ControlConfiguration source)
    {
        return new ImmutableControlConfiguration(source);
    }

    /**
     * Makes an independent, immutable copy of the specified configuration.
     * <p>
     * The returned object is completely independent of the original source.
     * <p>
     * This method is threadsafe but the source object being copied may not
     * be.  It is up to the caller to ensure that the configuration being
     * copied cannot be modified during the copying process.
     * 
     * @param source the configuration to copy
     * @return an independent, read-only copy of the configuration
     */
    final static ControllerConfiguration immutableControllerConfiguration(
            ControllerConfiguration source)
    {
        return new ImmutableControllerConfiguration(source);
    }

    /**
     * Returns a required float
     * (<strong>which must be in the form emitted by
     * {@link #floatToHexBitString(float)}</strong>)
     * from a map using the specified key.
     * 
     * @param map the map in which to perform the lookup
     * @param key the key under which the value is expected to live
     * @return the value bound to the specified key
     * @throws ConfigurationException if the value is not found or if the
     * value cannot be parsed as expected
     */
    final static float getFloat(Map<String,String> map, String key)
    throws ConfigurationException
    {
        String value = map.get(key);
        if (value == null)
        {
            throw new ConfigurationException("No such key: " + key);
        }

        try
        {
            return floatFromHexBitString(value);
        }
        catch(NumberFormatException e)
        {
            throw new ConfigurationException("Configuration is corrupt.  Key '"
                    + key + "' should refer to Float, but instead refers to "
                    + "value '" + value + "'");
        }
    }

    /**
     * Returns an optional float
     * (<strong>which must be in the form emitted by
     * {@link #floatToHexBitString(float)}</strong>, if present)
     * from a map using the specified key.
     * 
     * @param map the map in which to perform the lookup
     * @param key the key under which the value is expected to live
     * @param defaultValue default value to return if the value is not bound
     * to the specified key
     * @return the value bound to the specified key, or the default value
     * otherwise
     * @throws ConfigurationException if the
     * value cannot be parsed as expected
     */
    final static float getFloat(Map<String,String> map, String key,
            float defaultValue)
    throws ConfigurationException
    {
        String value = map.get(key);
        if (value == null)
        {
            return defaultValue;
        }

        try
        {
            return floatFromHexBitString(value);
        }
        catch(NumberFormatException e)
        {
            throw new ConfigurationException("Configuration is corrupt.  Key '"
                    + key + "' should refer to Float, but instead refers to "
                    + "value '" + value + "'");
        }
    }

    /**
     * Returns a required boolean from a map using the specified key.
     * 
     * @param map the map in which to perform the lookup
     * @param key the key under which the value is expected to live
     * @return the value bound to the specified key
     * @throws ConfigurationException if the value is not found or if the
     * value cannot be parsed as expected
     */
    final static boolean getBoolean(Map<String,String> map, String key)
    throws ConfigurationException
    {
        String value = map.get(key);
        if (value == null)
        {
            throw new ConfigurationException("No such key: " + key);
        }

        if (value.equalsIgnoreCase("true"))
        {
            return true;
        }
        else if (value.equalsIgnoreCase("false"))
        {
            return false;
        }
        else
        {
            throw new ConfigurationException("Configuration is corrupt.  Key '"
                    + key + "' should refer to Boolean, but instead refers to "
                    + "value '" + value + "'");
        }
    }

    /**
     * Returns an optional boolean from a map using the specified key.
     * 
     * @param map the map in which to perform the lookup
     * @param key the key under which the value is expected to live
     * @param defaultValue default value to return if the value is not bound
     * to the specified key
     * @return the value bound to the specified key, or the default value
     * otherwise
     * @throws ConfigurationException if the
     * value cannot be parsed as expected
     */
    final static boolean getBoolean(Map<String,String> map, String key,
            boolean defaultValue)
    throws ConfigurationException
    {
        String value = map.get(key);
        if (value == null)
        {
            return defaultValue;
        }

        if (value.equalsIgnoreCase("true"))
        {
            return true;
        }
        else if (value.equalsIgnoreCase("false"))
        {
            return false;
        }
        else
        {
            throw new ConfigurationException("Configuration is corrupt.  Key '"
                    + key + "' should refer to Boolean, but instead refers to "
                    + "value '" + value + "'");
        }
    }

    /**
     * Returns a required string from a map using the specified key.
     * 
     * @param map the map in which to perform the lookup
     * @param key the key under which the value is expected to live
     * @return the value bound to the specified key
     * @throws ConfigurationException if the value is not found or if the
     * value cannot be parsed as expected
     */
    final static String getString(Map<String,String> map, String key)
    throws ConfigurationException
    {
        String value = map.get(key);
        if (value == null)
        {
            throw new ConfigurationException("No such key: " + key);
        }
        return value;
    }

    /**
     * Returns an optional string from a map using the specified key.
     * 
     * @param map the map in which to perform the lookup
     * @param key the key under which the value is expected to live
     * @param defaultValue default value to return if the value is not bound
     * to the specified key
     * @return the value bound to the specified key, or the default value
     * otherwise
     */
    final static String getString(Map<String,String> map, String key,
            String defaultValue)
    {
        String value = map.get(key);
        if (value == null)
        {
            return defaultValue;
        }
        return value;
    }

    /**
     * Returns a required int from a map using the specified key.
     * 
     * @param map the map in which to perform the lookup
     * @param key the key under which the value is expected to live
     * @return the value bound to the specified key
     * @throws ConfigurationException if the value is not found or if the
     * value cannot be parsed as expected
     */
    final static int getInteger(Map<String,String> map, String key)
    throws ConfigurationException
    {
        String value = map.get(key);
        if (value == null)
        {
            throw new ConfigurationException("No such key: " + key);
        }

        try
        {
            return Integer.parseInt(value);
        }
        catch(NumberFormatException e)
        {
            throw new ConfigurationException("Configuration is corrupt.  Key '"
                    + key + "' should refer to Integer, but instead refers to "
                    + "value '" + value + "'");
        }
    }

    /**
     * Returns an optional int from a map using the specified key.
     * 
     * @param map the map in which to perform the lookup
     * @param key the key under which the value is expected to live
     * @param defaultValue default value to return if the value is not bound
     * to the specified key
     * @return the value bound to the specified key, or the default value
     * otherwise
     * @throws ConfigurationException if the
     * value cannot be parsed as expected
     */
    final static int getInteger(Map<String,String> map, String key,
            int defaultValue)
    throws ConfigurationException
    {
        String value = map.get(key);
        if (value == null)
        {
            return defaultValue;
        }

        try
        {
            return Integer.parseInt(value);
        }
        catch(NumberFormatException e)
        {
            throw new ConfigurationException("Configuration is corrupt.  Key '"
                    + key + "' should refer to Integer, but instead refers to "
                    + "value '" + value + "'");
        }
    }

    /**
     * Returns a required long from a map using the specified key.
     * 
     * @param map the map in which to perform the lookup
     * @param key the key under which the value is expected to live
     * @return the value bound to the specified key
     * @throws ConfigurationException if the value is not found or if the
     * value cannot be parsed as expected
     */
    final static long getLong(Map<String,String> map, String key)
    throws ConfigurationException
    {
        String value = map.get(key);
        if (value == null)
        {
            throw new ConfigurationException("No such key: " + key);
        }

        try
        {
            return Long.parseLong(value);
        }
        catch(NumberFormatException e)
        {
            throw new ConfigurationException("Configuration is corrupt.  Key '"
                    + key + "' should refer to Long, but instead refers to "
                    + "value '" + value + "'");
        }
    }

    /**
     * Returns an optional long from a map using the specified key.
     * 
     * @param map the map in which to perform the lookup
     * @param key the key under which the value is expected to live
     * @param defaultValue default value to return if the value is not bound
     * to the specified key
     * @return the value bound to the specified key, or the default value
     * otherwise
     * @throws ConfigurationException if the
     * value cannot be parsed as expected
     */
    final static long getLong(Map<String,String> map, String key,
            long defaultValue)
    throws ConfigurationException
    {
        String value = map.get(key);
        if (value == null)
        {
            return defaultValue;
        }

        try
        {
            return Long.parseLong(value);
        }
        catch(NumberFormatException e)
        {
            throw new ConfigurationException("Configuration is corrupt.  Key '"
                    + key + "' should refer to Long, but instead refers to "
                    + "value '" + value + "'");
        }
    }

    /**
     * Converts a floating point value to a raw bit value in hexadecimal
     * suitable for storage.
     * 
     * @param value the value to convert
     * @return a hexadecimal representation of the bit pattern of the floating
     * point value as if it were an unsigned int
     */
    final static String floatToHexBitString(float value)
    {
        return Integer.toHexString(Float.floatToRawIntBits(value));
    }

    /**
     * Converts a string previously generated by
     * {@link #floatToHexBitString(float)} back into the same exact
     * floating point representation (bit for bit) as the original.
     * 
     * @param value the value to convert back into a float
     * @return an exact copy of the float that was the input to
     * {@link #floatToHexBitString(float)}
     * @throws NumberFormatException if the string doesn't appear to be
     * a valid floating point number; note that this does not prevent
     * against all forms of corruption, since many valid strings will parse
     * to a floating point bit pattern in hex
     */
    final static float floatFromHexBitString(String value)
    throws NumberFormatException
    {
        long asLong = Long.parseLong(value, 16);
        // 0xFFFFFFFF00000000 is 32 bits of 1's followed bt 32 bits of 0's
        // That is, it is a mask for values greater than the largest possible
        // unsigned 32-bit integer
        if ( (asLong & 0xFFFFFFFF00000000L) != 0)
        {
            // String contains more than 32 bits of data!
            throw new NumberFormatException(
                    "Bit string representation contains more than 32 bits "
                    + "of data: " + value);
        }

        int lsb = (int) (asLong & 0x1L);
        int rawBits = (int) (asLong >> 1);
        rawBits <<= 1;
        rawBits |= lsb;
        return Float.intBitsToFloat(rawBits);
    }

    /**
     * An immutable, and therefore threadsafe, representation of a
     * component configuration.
     */
    private final static class ImmutableControlConfiguration
    extends ControlConfiguration
    {
        /**
         * Constructs an independent deep copy of the specified source object.
         * 
         * @param source the source object to copy from
         */
        ImmutableControlConfiguration(ControlConfiguration source)
        {
            super();
            userDefinedId = source.userDefinedId;
            deadZoneLowerBound = source.deadZoneLowerBound;
            deadZoneUpperBound = source.deadZoneUpperBound;
            granularity = source.granularity;
            isInverted = source.isInverted;
            isTurboEnabled = source.isTurboEnabled;
            centerValueOverride = source.centerValueOverride;
            valueIdsByValue.putAll(
                    source.valueIdsByValue);
        }

        /**
         * Constructs a shallow copy of the specified source object.
         * <p>
         * The new object has a different hashcode than the source but
         * shares its contents instead of making a pointless copy.
         * 
         * @param source the cource to copy
         */
        ImmutableControlConfiguration(ImmutableControlConfiguration source)
        {
            super();
            userDefinedId = source.userDefinedId;
            deadZoneLowerBound = source.deadZoneLowerBound;
            deadZoneUpperBound = source.deadZoneUpperBound;
            granularity = source.granularity;
            isInverted = source.isInverted;
            isTurboEnabled = source.isTurboEnabled;
            centerValueOverride = source.centerValueOverride;
            valueIdsByValue = source.valueIdsByValue;
        }

        /**
         * For internal use only.
         */
        private ImmutableControlConfiguration()
        {
            // Nothing
        }

        /* (non-Javadoc)
         * @see org.nicegamepads.ControllerConfiguration#clone()
         */
        protected final ControlConfiguration clone()
        throws CloneNotSupportedException
        {
            // Do NOT call super.clone.  This is purposeful!  Super.clone()
            // would create a MUTABLE INSTANCE of this class.
            ImmutableControlConfiguration clone =
                new ImmutableControlConfiguration();
            clone.centerValueOverride = centerValueOverride;
            clone.deadZoneLowerBound = deadZoneLowerBound;
            clone.deadZoneUpperBound = deadZoneUpperBound;
            clone.granularity = granularity;
            clone.isInverted = isInverted;
            clone.isTurboEnabled = isTurboEnabled;
            clone.turboDelayMillis = turboDelayMillis;
            clone.userDefinedId = userDefinedId;
            clone.valueIdsByValue.putAll(valueIdsByValue);
            return clone;
        }

        @Override
        public final void setCenterValueOverride(float centerValue)
        {
            throw new UnsupportedOperationException(
                "This configuration is read-only.");
        }

        @Override
        public final void setDeadZoneBounds(float lowerBound, float upperBound)
        {
            throw new UnsupportedOperationException(
                "This configuration is read-only.");
        }

        @Override
        public final void setGranularity(float granularity)
        {
            throw new UnsupportedOperationException(
                "This configuration is read-only.");
        }

        @Override
        public final void setInverted(boolean isInverted)
        {
            throw new UnsupportedOperationException(
                "This configuration is read-only.");
        }

        @Override
        public final void setTurboDelayMillis(long turboDelayMillis)
        {
            throw new UnsupportedOperationException(
                "This configuration is read-only.");
        }

        @Override
        public final void setTurboEnabled(boolean isTurboEnabled)
        {
            throw new UnsupportedOperationException(
                "This configuration is read-only.");
        }

        @Override
        public final void setUserDefinedId(int userDefinedId)
        {
            throw new UnsupportedOperationException(
                "This configuration is read-only.");
        }

        @Override
        public final void setValueId(float value, int id)
        {
            throw new UnsupportedOperationException(
                "This configuration is read-only.");
        }
    }

    /**
     * An immutable, and therefore threadsafe, representation of a controller
     * configuration.
     * 
     * @author Andrew Hayden
     */
    private final static class ImmutableControllerConfiguration
    extends ControllerConfiguration
    {
        /**
         * For internal use only.
         * 
         * @param source the source to copy from
         */
        ImmutableControllerConfiguration(ControllerConfiguration source)
        {
            super();
            // Copy will have blank cache of component locations by controller
            // config.  This is exactly what should happen, since we are creating
            // brand new config objects.
            NiceController controller = source.getController();

            // Generate fingerprint immediately.
            setController(controller);
            setControllerFingerprint(controller.getFingerprint());

            // Fill in config info for components
            List<NiceControl> controls = controller.getControls();
            LinkedHashMap<NiceControl, ControlConfiguration>
                newComponentConfigurations =
                    new LinkedHashMap<NiceControl, ControlConfiguration>(
                            controls.size());
            for (NiceControl control : controls)
            {
                newComponentConfigurations.put(
                        control,
                        new ImmutableControlConfiguration(
                                source.getControlConfigurations().get(
                                        control)));
            }
            setControlConfigurations(newComponentConfigurations);
        }

        /**
         * Constructs a shallow copy of the specified source object.
         * <p>
         * The new object has a different hashcode than the source but
         * shares its contents instead of making a pointless copy.
         * 
         * @param source the cource to copy
         */
        ImmutableControllerConfiguration(
                ImmutableControllerConfiguration source)
        {
            // Copy will share the cache of component locations by controller
            // config.  This is exactly what should happen, since we are sharing
            // existing config objects.
            NiceController controller = source.getController();
            setController(controller);
            setControllerFingerprint(source.getControllerFingerprint());
            setCachedConfigurationsByControl(
                    source.getCachedConfigurationsByControl());
            setControlConfigurations(source.getControlConfigurations());
        }

        /**
         * For internal use only.
         */
        private ImmutableControllerConfiguration()
        {
            // Do nothing.
        }

        /* (non-Javadoc)
         * @see org.nicegamepads.ControllerConfiguration#clone()
         */
        protected final ControllerConfiguration clone()
        throws CloneNotSupportedException
        {
            // Do NOT call super.clone.  This is purposeful!  Super.clone()
            // would create a MUTABLE INSTANCE of this class.
            ImmutableControllerConfiguration clone =
                new ImmutableControllerConfiguration();
            clone.setController(getController());
            clone.setControllerFingerprint(getControllerFingerprint());
            clone.setControlConfigurations(getControlConfigurations());
            return clone;
        }

        @Override
        public final void setConfiguration(NiceControl component,
                ControlConfiguration configuration)
                throws ConfigurationException
        {
            throw new UnsupportedOperationException(
                "This configuration is read-only.");
        }
    }
}