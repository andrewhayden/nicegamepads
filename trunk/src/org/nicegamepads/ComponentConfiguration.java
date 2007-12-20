package org.nicegamepads;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Logical configuration for a single component.
 * <p>
 * This class is <em>not</em> threadsafe.
 * 
 * @author Andrew Hayden
 */
public class ComponentConfiguration implements Cloneable
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
     * Whether or not turbo mode is enabled.
     */
    boolean isTurboEnabled;

    /**
     * How long, in milliseconds, after which a pushed button automatically
     * enters turbo mode.
     */
    long turboDelayMillis;

    /**
     * The value at the center of the range.
     */
    float centerValue;

    /**
     * "Map" whose keys are discrete floats produced by this control, if it
     * is digital, and whose values are the user-defined symbols associated
     * therewith.
     */
    final Map<Float, Integer> valueIdsByValue;

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
        isTurboEnabled = false;
        centerValue = 0f;
        valueIdsByValue = new HashMap<Float, Integer>();
    }

    /**
     * Constructs a copy of the specified configuration.
     * <p>
     * This is a safe alternative to calling {@link #clone()}, which may
     * be overridden by subclasses.
     */
    ComponentConfiguration(ComponentConfiguration source)
    {
        userDefinedId = source.userDefinedId;
        deadZoneLowerBound = source.deadZoneLowerBound;
        deadZoneUpperBound = source.deadZoneUpperBound;
        granularity = source.granularity;
        isInverted = source.isInverted;
        isTurboEnabled = source.isTurboEnabled;
        centerValue = source.centerValue;
        valueIdsByValue = new HashMap<Float, Integer>(source.valueIdsByValue);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    protected ComponentConfiguration clone()
    throws CloneNotSupportedException
    {
        super.clone();
        ComponentConfiguration clone = new ComponentConfiguration();
        clone.userDefinedId = userDefinedId;
        clone.deadZoneLowerBound = deadZoneLowerBound;
        clone.deadZoneUpperBound = deadZoneUpperBound;
        clone.granularity = granularity;
        clone.isInverted = isInverted;
        clone.isTurboEnabled = isTurboEnabled;
        clone.centerValue = centerValue;
        clone.valueIdsByValue.putAll(
                valueIdsByValue);
        return clone;
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
        destination.put(prefix + ".centerValue", ConfigurationUtils.floatToHexBitString(centerValue));
        destination.put(prefix + ".deadZoneLowerBound", ConfigurationUtils.floatToHexBitString(deadZoneLowerBound));
        destination.put(prefix + ".deadZoneUpperBound", ConfigurationUtils.floatToHexBitString(deadZoneUpperBound));
        destination.put(prefix + ".granularity", ConfigurationUtils.floatToHexBitString(granularity));
        destination.put(prefix + ".isInverted", Boolean.toString(isInverted));
        destination.put(prefix + ".isTurboEnabled", Boolean.toString(isTurboEnabled));
        destination.put(prefix + ".turboDelayMillis", Long.toString(turboDelayMillis));

        // Serialize list of values that are bound.
        StringBuilder buffer = new StringBuilder();
        Set<Float> keysAsFloat = new TreeSet<Float>(
                valueIdsByValue.keySet());
        Iterator<Float> keyIterator = keysAsFloat.iterator();
        while(keyIterator.hasNext())
        {
            Float key  = keyIterator.next();

            // Output the (key,value) pair for this binding
            String stringKey = ConfigurationUtils.floatToHexBitString(key);
            destination.put(prefix + ".userDefinedSymbolKey." + stringKey,
                    Integer.toString(valueIdsByValue.get(key)));

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
        centerValue = ConfigurationUtils.getFloat(
                source, prefix + ".centerValue");
        deadZoneLowerBound = ConfigurationUtils.getFloat(
                source, prefix + ".deadZoneLowerBound");
        deadZoneUpperBound = ConfigurationUtils.getFloat(
                source, prefix + ".deadZoneLowerBound");
        granularity = ConfigurationUtils.getFloat(
                source, prefix + ".granularity");
        isInverted = ConfigurationUtils.getBoolean(
                source, prefix + ".isInverted");
        isTurboEnabled = ConfigurationUtils.getBoolean(
                source, prefix + ".isTurboEnabled");
        turboDelayMillis = ConfigurationUtils.getLong(
                source, prefix + ".turboDelayMillis");

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
            float trueKey =
                ConfigurationUtils.floatFromHexBitString(userKeySerialized);
            int trueValue = ConfigurationUtils.getInteger(source,
                    prefix + ".userDefinedSymbolKey." + userKeySerialized);
            valueIdsByValue.put(trueKey, trueValue);
        }
    }

    /**
     * Returns all of the values that have IDs bound via
     * {@link #setValueId(float, int)}, in no particular order.
     * 
     * @return the values as an array (possibly of length zero but never
     * <code>null</code>)
     */
    public final float[] getAllValuesWithIds()
    {
        float[] values = new float[valueIdsByValue.size()];
        int counter = 0;
        for (float f : valueIdsByValue.keySet())
        {
            values[counter++] = f;
        }
        return values;
    }

    /**
     * Returns all of the IDs that have been vound to values via
     * {@link #setValueId(float, int)}, in no particular order.
     * <p>
     * Note that multiple values may be bound to the same ID.  The returned
     * array will not contain any duplicate values.
     * 
     * @return the ids as an array (possibly of length zero but never
     * <code>null</code>), excluding any duplicates
     */
    public final int[] getAllValueIds()
    {
        Set<Integer> asSet = new HashSet<Integer>(valueIdsByValue.values());
        int[] ids = new int[asSet.size()];
        int counter = 0;
        for (int i : asSet)
        {
            ids[counter++] = i;
        }
        return ids;
    }

    /**
     * Returns the user-defined ID for the specified value.
     * <p>
     * For more information about binding user-defined IDs to values,
     * please see {@link #setValueId(float, int)}.
     * 
     * @param value the value to look up
     * @return if a user-defined ID is bound to the specified value,
     * that ID; otherwise, {@link Integer#MIN_VALUE}
     * (this special value cannot be bound by the
     * {@link #setValueId(float, int)} method)
     */
    public final int getValueId(float value)
    {
        return valueIdsByValue.get(value);
    }

    /**
     * Binds a user-defined identifier to a specific value for this component.
     * <p>
     * The value {@link Integer#MIN_VALUE} is reserved and may not be used.
     * All other integer values are valid.
     * <p>
     * This method is (mainly) intended to be used for digtal components.
     * Digital components often emit fixed values that are useful
     * to enumerate and assign logical identifiers to.  For example, a digital
     * directional pad is often thought of as having exactly 8 possible values:
     * North, North-East, East, South-East, South, South-West, West, and
     * North-West; a typical digital button, however, might report these
     * human-intuitive concepts as the values 0.125, 0.250, 0.375, 0.500,
     * 0.625, 0.750, 0.875, and 1.000.  The values never change between runs,
     * and there is never any deviation in the bit pattern of the number
     * returned (that is, the floating point representation is <em>always
     * exactly the same, at a bit-for-bit level</em>).
     * <p>
     * This method binds such a value to an arbitrary value defined by
     * the caller, which can subsequently be used for taking the appropriate
     * action.  For a discussion of why this must be an integer (instead of
     * an enumeration, String, or other user-defined Object), please refer
     * to the documentation of {@link #setUserDefinedId(int)}.
     * <p>
     * Every time that the component is polled, the discovered value is
     * checked against the current bindings.  If there is a user-defined ID
     * bound to the specific value, that user-defined ID is associated with
     * the event that is fired (if any).
     * 
     * @param value the value to bind to a user-defined ID
     * @param id the user-defined ID that should be bound to this value
     * @throws IllegalArgumentException if the specified user ID is
     * {@link Integer#MIN_VALUE}, or if the specified value is outside the
     * range [-1, 1] (these values cannot occur and make no sense to bind)
     */
    public void setValueId(float value, int id)
    {
        if (value < -1.0f || 1.0f < value)
        {
            throw new IllegalArgumentException(
                    "Value must be in the range [-1.0,1.0]: " + value);
        }

        if (id == Integer.MIN_VALUE)
        {
            throw new IllegalArgumentException(
                    "ID " + Integer.MIN_VALUE + " is reserved and "
                    + "cannot be bound.");
        }

        valueIdsByValue.put(value, id);
    }

    /**
     * Returns the lower bound of the dead zone for this component.
     * 
     * @return if a valid bound has been set, that lower bound,
     * which must be in the range [-1.0, 1.0]; otherwise, {@link Float#NaN}
     */
    public final float getDeadZoneLowerBound()
    {
        return deadZoneLowerBound;
    }

    /**
     * Returns the upper bound of the dead zone for this component.
     * 
     * @return if a valid bound has been set, that lower bound,
     * which must be in the range [-1.0, 1.0]; otherwise, {@link Float#NaN}
     */
    public final float getDeadZoneUpperBound()
    {
        return deadZoneUpperBound;
    }

    /**
     * Sets or clears (or both) the bounds of the dead zone for this component.
     * <p>
     * The bound bounds must be in the range [-1.0, 1.0] if they are to be
     * considered valid.  To clear a bound, set its value to
     * {@link Float#NaN}.
     * <p>
     * If both bounds are set and the lower bound is greater than the upper
     * bound, an exception is thrown.  If both bounds are equal, this
     * indicates that there is only one dead zone value and it is the
     * value specified.
     * 
     * @param lowerBound the new lower bound to set.  If NaN, the
     * bound is cleared; otherwise, the bound is set
     * @param upperBound the new lower bound to set.  If NaN, the
     * bound is cleared; otherwise, the bound is set
     * @throws IllegalArgumentException if either of the specified bounds are
     * valid floating point numbers and are outside of the range [-1.0, 1.0],
     * or if either of the specified bounds is an infinite value, or if
     * the bounds are both valid but the lower bound is greater than the
     * upper bound
     */
    public void setDeadZoneBounds(float lowerBound, float upperBound)
    {
        float lowerBoundResult = lowerBound;
        float upperBoundResult = upperBound;

        if (Float.isInfinite(lowerBound)
                || (lowerBound < -1f || 1f < lowerBound))
        {
            throw new IllegalArgumentException(
                    "lower bound must be either NaN "
                    + "or a value in the range [-1,1]: " + lowerBound);
        }
        else if (Float.isNaN(lowerBound))
        {
            lowerBoundResult = Float.NaN;
        }

        if (Float.isInfinite(upperBound)
                || (upperBound < -1f || 1f < upperBound))
        {
            throw new IllegalArgumentException(
                    "upper bound must be either NaN "
                    + "or a value in the range [-1,1]: " + lowerBound);
        }
        else if (Float.isNaN(upperBound))
        {
            upperBoundResult = Float.NaN;
        }

        if (lowerBoundResult != Float.NaN && upperBoundResult != Float.NaN)
        {
            // Last sanity check
            if (lowerBound > upperBound)
            {
                throw new IllegalArgumentException(
                        "lower bound must be less than or equal to "
                        + "upper bound: " + lowerBound + " > " + upperBound);
            }
        }

        // Set bounds to NaN so that this method cannot cause a logical
        // error if the new lower bound is greater than the old upper bound,
        // or if the new upper bound is less than the old lower bound.
        // This is necessary because we aren't synchronizing here, so we
        // have to make certain we cannot cause an unexpected condition in
        // downstream code.
        deadZoneLowerBound = Float.NaN;
        deadZoneUpperBound = Float.NaN;

        // Set new lower and upper bounds.
        // Although this is not atomic, the bounds will always be logically
        // valid.  There is a brief window of opportunity during which the
        // lower bound could be read before the upper bound, but this
        // should generally not be a problem since the bounds will always be
        // in a valid (but perhaps incorrect) state
        deadZoneLowerBound = lowerBoundResult;
        deadZoneUpperBound = upperBoundResult;
    }

    /**
     * Returns the center value for this component.
     * 
     * @return the center value for this component
     * @see #setCenterValue(float)
     */
    public final float getCenterValue()
    {
        return centerValue;
    }

    /**
     * Sets the center value for this component.
     * <p>
     * The center value defaults to <code>0</code> and represents the neutral
     * position of the component.  Relative components may not have a center
     * value; for example, a wheel may produce only relative measurements
     * as it is turned and therefore may not have a "center" value, since there
     * is no notion of an absolute value in this sense.
     * <p>
     * The center value is important when considering granularity.
     * See {@link #setGranularity(float)} for more information.
     * 
     * @param centerValue the new center value to set
     */
    public void setCenterValue(float centerValue)
    {
        this.centerValue = centerValue;
    }

    /**
     * Returns the granularity of this component, if any.
     * 
     * @return if granularity has been set, the granularity, which must be
     * in the range [0,1]
     * @see #setGranularity(float)
     */
    public final float getGranularity()
    {
        return granularity;
    }

    /**
     * Sets or clears the granularity for this component.
     * <p>
     * The granularity is used to establish logical "bins" into which the
     * values produced by the component fall.  Crossing the boundary of
     * such a bin signals that a meaningful value change has taken place.
     * For example, if you have a highly sensitive component it may be able
     * to measure values to a precision of thousandths, such that a slight
     * breeze or vibration may cause the component to issue many value-changed
     * events for values such as .235, .236, .234 and so on;
     * setting a granularity allows the system to coalesce a range of values
     * into one logical "bin".  Only when the value crosses the boundary of
     * such a bin does the system dispatch a value-changed event.
     * <p>
     * For absoute components, granularity is always symmetric around the
     * center of the component (that is, components that don't produce
     * relative values like a wheel turning at a variable rate of speed).
     * <p>
     * For example, if the center value is zero (the default) and the
     * granularity is set to 0.25, then logical bins are established in
     * the following ranges:
     * <br>
     * <code>[-1.00, -0.75), [-0.75, -0.50), [-0.50, -0.25), [-0.25, 0.00),
     *       (0.00, 0.25], (0.25, 0.50], (0.50, 0.75], (0.75, 1.00]</code>
     * <p>
     * Notice that the actual center value is not included in any of these
     * ranges.  This makes sure that the component will notice a change
     * from one logical side of the component to the other logical side.
     * <p>
     * At first glance this would appear to be a problem, because the values
     * could fluctuate wildly around the center.  This is indeed true.  The
     * solution is to also configure a dead zone, using the method
     * {@link #setDeadZoneBounds(float, float)}, which will cause a specific
     * region of the component's values to coalesce to the center value.
     * See {@link #setDeadZoneBounds(float, float)} for more details.
     * <p>
     * Setting the granularity to {@link Float#NaN} causes any existing
     * granularity to be cleared.  If granularity is cleared, then no
     * binning is performed and every change of value that is detected by
     * the system may potentially generate a value-changed event.
     * 
     * @param granularity the new granularity to set, which must be in the
     * range [0, 1] or must be the value {@link Float#NaN}, in which case
     * the granularity is cleared
     */
    public void setGranularity(float granularity)
    {
        this.granularity = granularity;
    }

    /**
     * Returns whether or not the component is inverted.
     * 
     * @return <code>true</code> if the component is inverted; otherwise,
     * <code>false</code>
     */
    public final boolean isInverted()
    {
        return isInverted;
    }

    /**
     * Sets whether or not the component is inverted.
     * <p>
     * An inverted control essentially flips its endpoints.  In the case of
     * a button 0 becomes 1 and 1 becomes 0; in all other cases, the value
     * is simply multiplied by -1 (-1 becomes 1, and 1 becomes -1).
     * <p>
     * This feature is particularly useful in interfaces that involve vertical
     * movement, such as flight simulators and first-person-shooters;  this
     * can make programs simpler by removing the need for them to check
     * whether the component is inverted or not, and simply rely on this
     * configuration to swap the values for them (such that, for example,
     * "up" would become "down" and "down" would become "up").
     * 
     * @param isInverted whether or not the component should be inverted
     */
    public void setInverted(boolean isInverted)
    {
        this.isInverted = isInverted;
    }

    /**
     * Returns whether or not the component is in turbo mode.
     * 
     * @return <code>true</code> if the component is in turbo mode; otherwise,
     * <code>false</code>
     */
    public final boolean isTurboEnabled()
    {
        return isTurboEnabled;
    }

    /**
     * Sets whether or not the component is in turbo mode.
     * <p>
     * When turbo mode is <em>not enabled</em>, a single press of a button
     * generates a single button-pressed event when it is pressed, and a
     * single button-released event when it is released.  When turbo mode
     * <em>is enabled</em>, the button will generate a new button-pressed
     * event every single time that the component is polled, until the button
     * is released (which generates a single button-released event as usual).
     * <p>
     * This is useful for controls in systems where rapid button pushing
     * would be otherwise necessary.  This is particularly common in
     * applications that require repetitive actions, such as "shooters"
     * (e.g., space invaders et al).
     * <p>
     * Turbo mode takes effect immediately; if the button is already
     * pressed, it will start generating new button-pressed events as soon
     * as the configuration has been applied even before it is released.
     * That is, the user doesn't have to release the button before turbo
     * is activated.
     * <p>
     * When used in combination with {@link #setTurboDelayMillis(long)},
     * this can also allow for a combination of fine-grained and mass-event
     * behaviors in a semless, convenient integration.  See
     * {@link #setTurboDelayMillis(long)} for more information.
     * 
     * @param isTurboEnabled whether or not the component should be in
     * turbo mode
     */
    public void setTurboEnabled(boolean isTurboEnabled)
    {
        this.isTurboEnabled = isTurboEnabled;
    }

    /**
     * Returns the delay, in milliseconds, after which turbo behavior is
     * activated if turbo mode is enabled.
     * <p>
     * If turbo mode is not enabled, this value is meaningless.
     * <p>
     * See {@link #setTurboDelayMillis(long)} for more information.
     * 
     * @return the delay, in milliseconds
     * @see #setTurboDelayMillis(long)
     */
    public final long getTurboDelayMillis()
    {
        return turboDelayMillis;
    }

    /**
     * Sets an optional delay after which turbo mode behavior activates
     * so long as turbo mode is itself enabled.  <strong>If turbo mode
     * is not enabled, this field has no effect.</strong>
     * <p>
     * If the value is zero, then the delay is cleared; in this case,
     * turbo mode will fire a button-pressed event at every poll interval,
     * as usual.
     * <p>
     * If the value is positive then it represents the minimum amount of time
     * that the button must be in a "pressed" state before turbo mode will
     * start firing button-pressed events at every poll interval.  When the
     * button is released, the timer resets; the timer starts again the next
     * time the button is pressed.
     * <p>
     * If the button is currently pressed when this configuration is applied,
     * and the value is a positive integer, the timer starts immediately;
     * if the value is zero, any existing timer is canceled and the
     * turbo behavior is deactivated immediately.  If you subsequently set
     * a positive integer value while the button is <em>still</em> pressed,
     * everything should work as expected (the timer starts immediately again).
     * <p>
     * This behavior is useful in situations involving
     * discrete scrolling or movement, where the fine-grained detail of
     * single button presses is desired as well as the ability to rapidly
     * scroll or move through large swathes of content at a time.
     * <p>
     * Notice that the delay is specified as the <em>minimum</em> time that
     * must elapse.  The only hard guarantee on timing that can be made is
     * that the turbo behavior will start on or before the polling event
     * that occurs immediately after the specified amount of time has elapsed
     * and the button is still in a "pressed" state.  Put another way,
     * the finest precision this value can provide is the precision of the
     * polling interval.
     * 
     * @param turboDelayMillis must be either zero, in which case the
     * delay is cleared, or a positive integer representing the minimum
     * amount of time, in milliseconds, after which turbo mode takes
     * effect after a button pressed event
     */
    public void setTurboDelayMillis(long turboDelayMillis)
    {
        this.turboDelayMillis = turboDelayMillis;
    }

    /**
     * Returns the user-defined identifier for this component.
     * <p>
     * This value is an integer.  For a discussion of why the ID must be
     * an integer, please refer to {@link #setUserDefinedId(int)}.
     * 
     * @return the integer value that represents the user-defined ID of the
     * component associated with this configuration
     */
    public final int getUserDefinedId()
    {
        return userDefinedId;
    }

    /**
     * Sets the user-defined identifier for this component.
     * <p>
     * All events that are dispatched for the component that is associated
     * with this configuration will contain this ID value.
     * <p>
     * <strong>Why is the user defined ID an integer?</strong>
     * Briefly, there are a few reasons for this.  First, it is absolutely
     * not permissible for a mutable object to be used as an ID for thread
     * safety reasons, as the configuration may be accessed by threads
     * internal to this implementation.  Second, the id must be serializable
     * and should, for portability reasons, be unambiguous in its
     * serialization.  Finally, it is undesirable for the ID to be an instance
     * of class whose run-time type might not be available.
     * <p>
     * An integer has all of these properties, can be used in switch,
     * statements, takes up very little space, is easy to format as ASCII
     * text, and is efficient in terms of performing operations.  For these
     * reasons, the user ID is forced to be an integer.
     * <p>
     * For posterity, it should be noted that both enumerations and strings
     * were also considered, and were discarded (enumerations because they
     * might not be present at run-time, strings because they are slow for
     * comparison).
     * 
     * @param userDefinedId the user-defined ID to set.  It is
     * <em>strongly suggested</em> that this value be unique across the
     * entire controller, but this is not enforced by the implementation.
     * The specified ID is provided to all listeners in all events raised
     * by the component associated with this configuration.
     */
    public void setUserDefinedId(int userDefinedId)
    {
        this.userDefinedId = userDefinedId;
    }

    /**
     * Not for public use.
     * <p>
     * Returns a reference to the live map of values.
     * 
     * @return a reference to the live map
     */
    final Map<Float, Integer> getValueIdsByValue()
    {
        return valueIdsByValue;
    }
}