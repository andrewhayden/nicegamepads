package org.nicegamepads;

import net.java.games.input.Component;
import net.java.games.input.Controller;

/**
 * Interface for entities wishing to be notified about calibration events.
 * 
 * @author Andrew Hayden
 */
public interface CalibrationListener
{
    /**
     * Invoked when calibration is started.
     * 
     * @param controller the controller being calibrated
     */
    public abstract void calibrationStarted(Controller controller);

    /**
     * Invoked when calibration is stopped.
     * 
     * @param controller the controller being calibrated
     * @param results the current results
     */
    public abstract void calibrationStopped(Controller controller, CalibrationResults results);

    /**
     * Invoked when calibration results are updated.  Results are updated
     * in near-realtime as new values are discovered from a component.
     * 
     * @param controller the controller being calibrated
     * @param component the component whose range has been updated
     * @param range the new range
     */
    public abstract void calibrationResultsUpdated(Controller controller,
            Component component, CalibrationResults.Range range);
}
