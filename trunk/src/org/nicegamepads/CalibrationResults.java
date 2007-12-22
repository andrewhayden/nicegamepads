package org.nicegamepads;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.java.games.input.Component;
import net.java.games.input.Controller;

/**
 * Encapsulates the results of a calibration operation.
 * <p>
 * This class and all of its methods are threadsafe.
 * 
 * @author Andrew Hayden
 */
public final class CalibrationResults
{
    /**
     * Simple container for a range.
     * 
     * @author Andrew Hayden
     */
    public final static class Range
    {
        /**
         * Low value of the range.
         */
        public float low;

        /**
         * High value of the range.
         */
        public float high;

        /**
         * Whether or not the range represents a singularity (where the
         * low and high values are equal).
         */
        public boolean isSingularity;

        /**
         * The size of the range, for convenience, calculated as
         * <code>Math.abs(high - low)</code>.
         */
        public float size;

        /**
         * Constructs a new range with the specified values.
         * 
         * @param low the low value
         * @param high the high value
         */
        public Range(float low, float high)
        {
            this.low = low;
            this.high = high;
            this.isSingularity = (low == high);
            this.size = Math.abs(high - low);
        }

        /**
         * Copies another range.
         * 
         * @param source the range to copy from
         */
        public Range(Range source)
        {
            this(source.low, source.high);
        }

        @Override
        public final String toString()
        {
            return CalibrationResults.Range.class.getName()
                + ": [" + low + "," + high + "]";
        }
    }

    /**
     * All ranges by component.
     */
    private final Map<Component, Range> rangesByComponent;

    /**
     * The controller being calibrated.
     */
    private final Controller controller;

    /**
     * Constructs a new set of calibration results.
     */
    public CalibrationResults(Controller controller)
    {
        this.controller = controller;
        rangesByComponent = Collections.synchronizedMap(
                new HashMap<Component, Range>());
    }

    /**
     * Constructs a new, independent copy of the specified source.
     * 
     * @param source the source to copy from
     */
    public CalibrationResults(CalibrationResults source)
    {
        synchronized(source.rangesByComponent)
        {
            this.controller = source.controller;
            this.rangesByComponent = source.getResults();
        }
    }

    /**
     * Returns an independent copy of the range for the specified component.
     * 
     * @param component the component to look up the range for
     * @return an independent copy of the range for the specified component,
     * if any has been recorded; otherwise, <code>null</code>
     */
    public final Range getRange(Component component)
    {
        Range range = rangesByComponent.get(component);
        if (range != null)
        {
            return new Range(range);
        }
        return null;
    }

    /**
     * Returns a list of all the components that currently have ranges
     * in this result.
     * 
     * @return such a list
     */
    public final Collection<Component> getComponentsSeen()
    {
        synchronized(rangesByComponent)
        {
            return new ArrayList<Component>(rangesByComponent.keySet());
        }
    }

    /**
     * Returns a list of the components that currently have ranges of
     * either singularity or non-singularity nature (as specified).
     * <p>
     * Ranges that are singularities represent a mathematical point;
     * that is, <code>low == high</code> and so the size of the range is zero.
     * <p>
     * Ranges that are not singularities have a positive range size,
     * i.e. <code>low != high</code>.
     * 
     * @param singularities whether components with singularity ranges
     * should be returned
     * @return a map of components whose ranges are either singularities
     * (if <code>singularities==true</code>) or not
     * (if <code>singularities==false</code>)
     */
    public final Map<Component, Range> getResultsByRangeType(
            boolean singularities)
    {
        synchronized(rangesByComponent)
        {
            Map<Component, Range> results = new HashMap<Component, Range>();
            for (Map.Entry<Component, Range> entry : rangesByComponent.entrySet())
            {
                Range range = entry.getValue();
                if (range.isSingularity == singularities)
                {
                    results.put(entry.getKey(), new Range(range));
                }
            }
            return results;
        }
    }

    /**
     * Returns a list of the components that currently have ranges
     * where either endpoint is not one of the values in the set {-1,0,1}.
     * <p>
     * Generally speaking most components are normalized to return maximum
     * and minimum values that are one of the values {-1,0,1}.
     * This method finds components that don't appear to be behaving in this
     * manner based on the largest and smallest values seen.
     * 
     * @return a map of components whose ranges appear to be non-standard
     */
    public final Map<Component, Range> getNonStandardResults()
    {
        synchronized(rangesByComponent)
        {
            Map<Component, Range> results = new HashMap<Component, Range>();
            for (Map.Entry<Component, Range> entry : rangesByComponent.entrySet())
            {
                Range range = entry.getValue();
                if (!(
                        (range.high != 0f 
                        && range.high != -1f
                        && range.high != 1f)
                        ||
                        (range.low != 0f 
                        && range.low != -1f
                        && range.low != 1f)))
                {
                    results.put(entry.getKey(), new Range(range));
                }
            }
            return results;
        }
    }

    /**
     * Returns a copy of the results.
     * <p>
     * Each entry in the returned map consists of a component and the range
     * seen for that component.
     * 
     * @return a copy of the results
     */
    public final Map<Component, Range> getResults()
    {
        synchronized(rangesByComponent)
        {
            Map<Component, Range> results = new HashMap<Component, Range>(
                    rangesByComponent.size());
            for (Map.Entry<Component, Range> entry : rangesByComponent.entrySet())
            {
                results.put(entry.getKey(), new Range(entry.getValue()));
            }
            return results;
        }
    }

    /**
     * Sets the range for the specified component.
     * 
     * @param component the component to set the range for
     * @param range the range to set
     */
    final void setRange(Component component, Range range)
    {
        rangesByComponent.put(component, range);
    }

    /**
     * Clears all data from the results.
     */
    final void clear()
    {
        rangesByComponent.clear();
    }

    /**
     * Processes a value and updates the appropriate range as necessary.
     * 
     * @param component the component from which the value was recorded
     * @param value the value recorded; infinities and floats that are
     * representations of non-a-number (NaN) are ignored
     * @return <code>true</code> if a range was created or updated as a
     * result of this operation; otherwise, <code>false</code>
     */
    final boolean processValue(final Component component, float value)
    {
        // Cannot process infinite or NaN values
        if (Float.isInfinite(value) || Float.isNaN(value))
        {
            return false;
        }

        synchronized(rangesByComponent)
        {
            final boolean updated;
            Range range = rangesByComponent.get(component);
            if (range == null)
            {
                rangesByComponent.put(component, new Range(value, value));
                updated = true;
            }
            else
            {
                if (value < range.low)
                {
                    range.low = value;
                    updated = true;
                }
                else if (value > range.high)
                {
                    range.high = value;
                    updated = true;
                }
                else
                {
                    updated = false;
                }
            }

            return updated;
        }
    }

    /**
     * The controller being calibrated.
     * 
     * @return the controller being calibrated
     */
    public final Controller getController()
    {
        return controller;
    }

    @Override
    public final String toString()
    {
        StringBuilder buffer = new StringBuilder();
        Map<Component, Range> results = getResults();
        buffer.append(CalibrationResults.class.getName());
        buffer.append(": [");
        buffer.append("controller=");
        buffer.append(controller);
        buffer.append("]\nRanges by component:\n");
        Iterator<Map.Entry<Component, Range>> iterator =
            results.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<Component, Range> entry = iterator.next();
            buffer.append("    ");
            buffer.append(entry.getKey());
            buffer.append("=");
            buffer.append(entry.getValue());
            if (iterator.hasNext())
            {
                buffer.append("\n");
            }
        }
        return buffer.toString();
    }
}