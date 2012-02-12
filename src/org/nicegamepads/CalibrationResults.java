package org.nicegamepads;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
     * All ranges by control.
     */
    private final Map<NiceControl, Range> rangesByControl;

    /**
     * The controller being calibrated.
     */
    private final NiceController controller;

    /**
     * Constructs a new set of calibration results.
     */
    public CalibrationResults(NiceController controller)
    {
        this.controller = controller;
        rangesByControl = Collections.synchronizedMap(
                new HashMap<NiceControl, Range>());
    }

    /**
     * Constructs a new, independent copy of the specified source.
     * 
     * @param source the source to copy from
     */
    public CalibrationResults(CalibrationResults source)
    {
        synchronized(source.rangesByControl)
        {
            this.controller = source.controller;
            this.rangesByControl = source.getResults();
        }
    }

    /**
     * Returns an independent copy of the range for the specified component.
     * 
     * @param control the control to look up the range for
     * @return an independent copy of the range for the specified control,
     * if any has been recorded; otherwise, <code>null</code>
     */
    public final Range getRange(NiceControl control)
    {
        Range range = rangesByControl.get(control);
        if (range != null)
        {
            return new Range(range);
        }
        return null;
    }

    /**
     * Returns a list of all the controls that currently have ranges
     * in this result.
     * 
     * @return such a list
     */
    public final Collection<NiceControl> getComponentsSeen()
    {
        synchronized(rangesByControl)
        {
            return new ArrayList<NiceControl>(rangesByControl.keySet());
        }
    }

    /**
     * Returns a list of the controls that currently have ranges of
     * either singularity or non-singularity nature (as specified).
     * <p>
     * Ranges that are singularities represent a mathematical point;
     * that is, <code>low == high</code> and so the size of the range is zero.
     * <p>
     * Ranges that are not singularities have a positive range size,
     * i.e. <code>low != high</code>.
     * 
     * @param singularities whether controls with singularity ranges
     * should be returned
     * @return a map of controls whose ranges are either singularities
     * (if <code>singularities==true</code>) or not
     * (if <code>singularities==false</code>)
     */
    public final Map<NiceControl, Range> getResultsByRangeType(
            boolean singularities)
    {
        synchronized(rangesByControl)
        {
            Map<NiceControl, Range> results = new HashMap<NiceControl, Range>();
            for (Map.Entry<NiceControl, Range> entry : rangesByControl.entrySet())
            {
                Range range = entry.getValue();
                if (range.isSingularity() == singularities)
                {
                    results.put(entry.getKey(), new Range(range));
                }
            }
            return results;
        }
    }

    /**
     * Returns a list of the controls that currently have ranges
     * where either endpoint is not one of the values in the set {-1,0,1}.
     * <p>
     * Generally speaking most controls are normalized to return maximum
     * and minimum values that are one of the values {-1,0,1}.
     * This method finds controls that don't appear to be behaving in this
     * manner based on the largest and smallest values seen.
     * 
     * @return a map of controls whose ranges appear to be non-standard
     */
    public final Map<NiceControl, Range> getNonStandardResults()
    {
        synchronized(rangesByControl)
        {
            final Map<NiceControl, Range> results = new HashMap<NiceControl, Range>();
            for (Map.Entry<NiceControl, Range> entry : rangesByControl.entrySet())
            {
                Range range = entry.getValue();
                final float high = range.getHigh();
                final float low = range.getLow();
                if (!((high != 0f  && high != -1f && high != 1f)
                        || (low != 0f  && low != -1f && low != 1f))) {
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
    public final Map<NiceControl, Range> getResults()
    {
        synchronized(rangesByControl)
        {
            Map<NiceControl, Range> results = new HashMap<NiceControl, Range>(
                    rangesByControl.size());
            for (Map.Entry<NiceControl, Range> entry : rangesByControl.entrySet())
            {
                results.put(entry.getKey(), new Range(entry.getValue()));
            }
            return results;
        }
    }

    /**
     * Sets the range for the specified control.
     * 
     * @param control the control to set the range for
     * @param range the range to set
     */
    final void setRange(NiceControl control, Range range)
    {
        rangesByControl.put(control, range);
    }

    /**
     * Clears all data from the results.
     */
    final void clear()
    {
        rangesByControl.clear();
    }

    /**
     * Processes a value and updates the appropriate range as necessary.
     * 
     * @param control the control from which the value was recorded
     * @param value the value recorded; infinities and floats that are
     * representations of non-a-number (NaN) are ignored
     * @return <code>true</code> if a range was created or updated as a
     * result of this operation; otherwise, <code>false</code>
     */
    final boolean processValue(final NiceControl control, float value)
    {
        // Cannot process infinite or NaN values
        if (Float.isInfinite(value) || Float.isNaN(value))
        {
            return false;
        }

        synchronized(rangesByControl)
        {
            final boolean updated;
            Range range = rangesByControl.get(control);
            if (range == null)
            {
                rangesByControl.put(control, new Range(value, value));
                updated = true;
            }
            else
            {
                if (value < range.getLow()) {
                    rangesByControl.put(control, new Range(value, range.getHigh()));
                    updated = true;
                }
                else if (value > range.getHigh()) {
                    rangesByControl.put(control, new Range(range.getLow(), value));
                    updated = true;
                } else {
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
    public final NiceController getController()
    {
        return controller;
    }

    @Override
    public final String toString()
    {
        StringBuilder buffer = new StringBuilder();
        Map<NiceControl, Range> results = getResults();
        buffer.append(CalibrationResults.class.getName());
        buffer.append(": [");
        buffer.append("controller=");
        buffer.append(controller);
        buffer.append("]\nRanges by component:\n");
        Iterator<Map.Entry<NiceControl, Range>> iterator =
            results.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<NiceControl, Range> entry = iterator.next();
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