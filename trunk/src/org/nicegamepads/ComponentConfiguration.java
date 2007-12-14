package org.nicegamepads;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class ComponentConfiguration
{
    /**
     * The lower bound of the dead zone, if any (inclusive).
     */
    float deadZoneLowerBound;

    /**
     * The upper bound of the dead zone, if any (inclusive).
     */
    float deadZoneUpperBound;

    /**
     * The granularity, if any.
     */
    float granularity;

    /**
     * Whether or not this is an inverted configuration.
     */
    boolean isInverted;

    /**
     * "Map" whose keys are discrete floats produced by this control, if it
     * is digital, and whose values are the user-defined symbols associated
     * therewith.
     */
    Map<Float, Integer> userDefinedSymbolsByDigitalValue;

    /**
     * User-defined ID for this component.
     */
    int userDefinedId;

    /**
     * Creates a new, empty configuration with default values.
     */
    ComponentConfiguration()
    {
        userDefinedId = 0;
        deadZoneLowerBound = 0f;
        deadZoneUpperBound = 0f;
        granularity = 0f;
        isInverted = false;
        userDefinedSymbolsByDigitalValue = new HashMap<Float, Integer>();
    }

    /**
     * Saves this partial configuration to a mapping of (key,value) pairs
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

        destination.put(prefix + ".userDefinedId", Integer.toString(userDefinedId));
        destination.put(prefix + ".deadZoneLowerBound", Float.toString(deadZoneLowerBound));
        destination.put(prefix + ".deadZoneUpperBound", Float.toString(deadZoneUpperBound));
        destination.put(prefix + ".granularity", Float.toString(granularity));
        destination.put(prefix + ".isInverted", Boolean.toString(isInverted));

        // Serialize list of values that are bound.
        // This is tricky because the keys are float values.
        // To guarantee that these don't lose precision between runs, we need
        // to store them in an unambiguous form.  To do this we use Java's
        // ability to extract the raw IEEE bits for the float, which we then
        // write as a hex string.
        StringBuilder buffer = new StringBuilder();
        Set<Float> keysAsFloat = new TreeSet<Float>(
                userDefinedSymbolsByDigitalValue.keySet());
        Iterator<Float> keyIterator = keysAsFloat.iterator();
        while(keyIterator.hasNext())
        {
            Float key  = keyIterator.next();

            // Output the (key,value) pair for this binding
            String stringKey = Integer.toHexString(Float.floatToRawIntBits(key));
            destination.put(prefix + ".userDefinedSymbolKey." + stringKey,
                    Integer.toString(userDefinedSymbolsByDigitalValue.get(key)));

            // Then add the key itself to the running list of keys.
            buffer.append(stringKey);
            if (keyIterator.hasNext())
            {
                buffer.append(",");
            }
        }
        destination.put(prefix + ".userDefinedSymbolKeysList",
                buffer.toString());

        return destination;
    }

    /**
     * Loads this partial configuration from Java properties.
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
        userDefinedId = ConfigurationUtils.getInteger(
                source, prefix + ".userDefinedId");
        deadZoneLowerBound = ConfigurationUtils.getFloat(
                source, prefix + ".deadZoneLowerBound");
        deadZoneUpperBound = ConfigurationUtils.getFloat(
                source, prefix + ".deadZoneLowerBound");
        granularity = ConfigurationUtils.getFloat(
                source, prefix + ".granularity");
        isInverted = ConfigurationUtils.getBoolean(
                source, prefix + ".isInverted");

        // Read in the property that lists all Float keys for our
        // user-defined symbols table.
        String userKeysSerializedList = ConfigurationUtils.getString(
                source, prefix + ".userDefinedSymbolKeysList");
        String[] userKeysSerializedArray = userKeysSerializedList.split(",");

        // For each key we found, convert the key back into a IEEE Float and
        // lookup the corresponding (key,value) pair in the properties to
        // find the user-defined symbol value for that key.
        for (String userKeySerialized : userKeysSerializedArray)
        {
            long asLong = Long.parseLong(userKeySerialized, 16);
            int lsb = (int) (asLong & 0x1L);
            int rawBits = (int) (asLong >> 1);
            rawBits <<= 1;
            rawBits |= lsb;
            float trueKey = Float.intBitsToFloat(rawBits);
            int trueValue = ConfigurationUtils.getInteger(source,
                    prefix + ".userDefinedSymbolKey." + userKeySerialized);
            userDefinedSymbolsByDigitalValue.put(trueKey, trueValue);
        }
    }
}