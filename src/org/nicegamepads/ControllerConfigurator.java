package org.nicegamepads;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.nicegamepads.CalibrationResults.Range;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Component.Identifier;

/**
 * A useful tool for configuring a controller.
 * 
 * @author Andrew Hayden
 */
public class ControllerConfigurator
{
    /**
     * The controller to configure.
     */
    private final Controller controller;

    /**
     * The configuration that will be generated by this configurator.
     */
    private final ControllerConfiguration config;

    /**
     * Components that can be configured by this configurator.
     */
    private final Set<Component> eligibleComponents;

    /**
     * Synchronization lock.
     */
    private final Object lock = new Object();

    /**
     * The thread that is waiting for identification, if any.
     */
    private volatile Thread identificationThread = null;

    /**
     * The current calibration listener, if any.
     */
    private volatile CalibrationHelper calibrationHelper = null;

    /**
     * Poller used to poll the controller.
     */
    private final ControllerPoller poller;

    /**
     * Whether or not we are currently calibrating.
     */
    private boolean calibrating = false;

    /**
     * Listeners.
     */
    private final List<CalibrationListener> calibrationListeners
        = new CopyOnWriteArrayList<CalibrationListener>();

    /**
     * Constructs a new configurator to configure the specified controller.
     * 
     * @param controller the controller to be configured
     */
    public ControllerConfigurator(Controller controller)
    {
        this(controller, null);
    }

    /**
     * Constructs a new configurator to configure the specified controller,
     * optionally using a specified configuration to provide defaults.
     * <p>
     * If an optional default configuration is specified, a copy of that
     * configuration is made and serves as the basis for the configurator
     * operations.  This is a convenient way to "inherit" an existing
     * configuration and only update it piecemeal.
     * 
     * @param controller the controller to be configured
     * @param (optional) a default configuration to copy before configuration
     * begins
     */
    public ControllerConfigurator(Controller controller,
            ControllerConfiguration defaultConfiguration)
    {
        this(controller, defaultConfiguration, true);
    }

    /**
     * Constructs a new configurator to configure the specified controller,
     * optionally using a specified configuration to provide defaults.
     * <p>
     * If an optional default configuration is specified, a copy of that
     * configuration is made and serves as the basis for the configurator
     * operations.  This is a convenient way to "inherit" an existing
     * configuration and only update it piecemeal.
     * <p>
     * If the 
     * 
     * @param controller the controller to be configured
     * @param defaultConfiguration (optional) a default configuration
     * to copy before configuration begins
     * @param deep if <code>true</code>, the configurator will allow any
     * and all subcontroller components to participate; if <code>false</code>,
     * only components that are direct children of the specified controller
     * are considered.
     */
    public ControllerConfigurator(Controller controller,
            ControllerConfiguration defaultConfiguration, boolean deep)
    {
        this.controller = controller;
        if (defaultConfiguration == null)
        {
            this.config = new ControllerConfiguration(controller);
        }
        else
        {
            this.config = new ControllerConfiguration(defaultConfiguration);
        }
        eligibleComponents = new HashSet<Component>(
                ControllerUtils.getComponents(controller, deep));
        poller = new ControllerPoller(controller, deep);
    }

    /**
     * Adds a listner to be notified of calibration events.
     * 
     * @param listener the listener to add
     */
    public final void addCalibrationListener(CalibrationListener listener)
    {
        calibrationListeners.add(listener);
    }

    /**
     * Removes a previously-registered listner.
     * 
     * @param listener the listener to remove
     */
    public final void removeCalibrationListener(CalibrationListener listener)
    {
        calibrationListeners.remove(listener);
    }

    /**
     * Convenience method to call {@link #identifyComponent(long, TimeUnit)}
     * without a timeout (wait forever).
     * 
     * @return see {@link #identifyComponent(long, TimeUnit)}
     * @throws InterruptedException if interrupted while waiting
     */
    public final ComponentEvent identifyComponent()
    throws InterruptedException
    {
        return identifyComponent(0L, null);
    }

    /**
     * Attempts to synchronously identify a component by waiting up to
     * a specified amount of time for a qualifying event to be generated
     * by the controller.
     * <p>
     * A qualifying event is an event that clearly indicates that the device
     * has received interesting input.  This is defined as follows:
     * <ol>
     *  <li>For buttons and keys: the value of a component reaches the value
     *      1.0f and then returns to 0.0f.</li>
     *  <li>For relative axes: the value of a component reaches any non-zero
     *      value (e.g., wheel is turned, mouse is moved, etc)</li>
     *  <li>For point-of-view hats: the value of a component reaches any
     *      non-zero value and then returns to 0.0f.</li>
     *  <li>Everything else: the value of a component reaches either -1.0f
     *      of 1.0f, and then returns to 0.0f.  This is generally analgous
     *      to the axis being "pushed" all the way to one of its limits and
     *      then released.</li> 
     * </ol>
     * <p>
     * The return value of this method is a {@link ComponentEvent}.
     * This return value, when not <code>null</code>, contains a complete
     * description of the qualifying event.  The <code>previousValue</code>
     * field of the event is set to the non-zero value that qualified the
     * event.  The <code>currentValue</code> contains the final value was that
     * completed the qualification (in the case of relative axes, the same
     * non-zero value is in both fields; in all other cases, the current value
     * should be 0.0f since that is the only value that can complete a
     * qualifying event).
     * 
     * @return if a qualifying event occurs before the timeout period, the
     * event that contains the qualifying event; otherwise, <code>null</code>
     * @throws InterruptedException  if interrupted while waiting
     */
    public final ComponentEvent identifyComponent(long timeout, TimeUnit unit)
    throws InterruptedException
    {
        synchronized(lock)
        {
            if (identificationThread != null)
            {
                throw new IllegalStateException("Already identifying.");
            }
            if (calibrating)
            {
                throw new IllegalStateException("Already calibrating.");
            }

            identificationThread = Thread.currentThread();

            CountDownLatch latch = new CountDownLatch(1);
            IdentificationListener myListener = new IdentificationListener(latch);
            poller.addComponentPollingListener(myListener);
            poller.setConfiguration(config);

            // TODO: configurable polling interval
            poller.startPolling(33L, TimeUnit.MILLISECONDS);

            boolean success = false;
            try
            {
                if (timeout != 0L)
                {
                    success = latch.await(timeout, unit);
                }
                else
                {
                    latch.await();
                    success = true;
                }
            }
            finally
            {
                // Always remove the listener and halt polling!
                poller.removeComponentPollingListener(myListener);
                poller.stopPolling();
                identificationThread = null;
            }

            if (success)
            {
                return myListener.winner;
            }
            else
            {
                return null;
            }
        }
    }

    /**
     * Begins calibration.
     * <p>
     * During calibration, the high and low values for each component are
     * tracked constantly.  The highest and lowest values seen during
     * calibration are available 
     */
    public final void startCalibrating()
    {
        synchronized(lock)
        {
            if (identificationThread != null)
            {
                throw new IllegalStateException("Already identifying.");
            }
            if (calibrating)
            {
                throw new IllegalStateException("Already calibrating.");
            }

            // Start calibration.
            calibrating = true;
            calibrationHelper = new CalibrationHelper();
            calibrationHelper.start();
            poller.addComponentPollingListener(calibrationHelper);
            poller.setConfiguration(config);

            ControllerManager.eventDispatcher.submit(new LoggingRunnable(){
                @Override
                protected void runInternal()
                {
                    for (CalibrationListener listener : calibrationListeners)
                    {
                        listener.calibrationStarted(controller);
                    }
                }
            });

            // TODO: configurable polling interval
            poller.startPolling(33L, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops calibrating and returns the results of calibration.
     * 
     * @return the results of calibration
     */
    public final CalibrationResults stopCalibrating()
    {
        synchronized(lock)
        {
            if (!calibrating)
            {
                throw new IllegalStateException("Not currently calibrating.");
            }

            // Stop calibration.
            calibrationHelper.stop();
            poller.removeComponentPollingListener(calibrationHelper);
            poller.stopPolling();
            calibrating = false;

            ControllerManager.eventDispatcher.submit(new LoggingRunnable(){
                @Override
                protected void runInternal()
                {
                    for (CalibrationListener listener : calibrationListeners)
                    {
                        listener.calibrationStopped(
                                controller,
                                new CalibrationResults(
                                        calibrationHelper.results));
                    }
                }
            });

            return calibrationHelper.results;
        }
    }
    /**
     * Listens for polling events and identifies the first component to
     * reach the end of its range and return to neutral.
     * 
     * @author Andrew Hayden
     */
    private final class IdentificationListener implements ComponentPollingListener
    {
        /**
         * Map of whether or not a bound has been reached, by related
         * component.
         */
        private final Map<Component, Boolean> boundsReachedByComponent =
            Collections.synchronizedMap(new HashMap<Component, Boolean>());

        /**
         * The value that qualified the associated component as a potential
         * winner.
         */
        private final Map<Component, Float> qualifyingValueByComponent =
            Collections.synchronizedMap(new HashMap<Component, Float>());

        /**
         * Lock used for synchronization.
         */
        private final CountDownLatch latch;

        /**
         * The component that has won.
         */
        private volatile ComponentEvent winner = null;

        /**
         * Constructs a new stateful listener.
         * 
         * @param latch the latch to count down when done
         */
        IdentificationListener(CountDownLatch latch)
        {
            this.latch = latch;
        }

        @Override
        public final void componentPolled(ComponentEvent event)
        {
            //System.out.println(event);
            if (event.sourceComponent == null || winner != null
                    || !eligibleComponents.contains(event.sourceComponent))
            {
                return; // unknown component, or already done.  stop.
            }

            boolean boundsHit = false;

            // Scoping block.  Don't want 'test' hanging out.
            {
                Boolean test = 
                    boundsReachedByComponent.get(event.sourceComponent);
                if (test != null)
                {
                    boundsHit = test;
                }
            }

            if (event.sourceComponentType == ComponentType.BUTTON
                    || event.sourceComponentType == ComponentType.KEY)
            {
                if (!boundsHit)
                {
                    // Wait for value to hit 1.0, then return to 0.
                    if (event.currentValue == 1.0f)
                    {
                        boundsReachedByComponent.put(
                                event.sourceComponent, Boolean.TRUE);
                        qualifyingValueByComponent.put(
                                event.sourceComponent, event.currentValue);
                    }
                }
                else
                {
                    // Value has already hit 1.0.  Wait for it to return to 0f.
                    if (event.currentValue == 0.0f)
                    {
                        // Winner!
                        winner = event;
                    }
                }
            }
            else if (event.sourceComponentType == ComponentType.AXIS)
            {
                if (event.sourceComponent.getIdentifier() == Identifier.Axis.POV)
                {
                    // POV hat controls have discrete values.
                    // Any non-zero value will fulfill the bounds check.
                    if (!boundsHit)
                    {
                        // Wait for value to hit any non-zeroo value and return
                        // to zero.
                        if (event.currentValue != 0.0f)
                        {
                            boundsReachedByComponent.put(
                                    event.sourceComponent, Boolean.TRUE);
                            qualifyingValueByComponent.put(
                                    event.sourceComponent, event.currentValue);
                        }
                    }
                    else
                    {
                        // Value has already hit a non-zero value;
                        // Wait for it to return to 0f.
                        if (event.currentValue == 0.0f)
                        {
                            // Winner!
                            winner = event;
                        }
                    }
                }
                else if (event.sourceComponent.isRelative())
                {
                    // Relative controls may never hit their range.
                    // Any non-zero value will fulfill the bounds check.
                    if (event.currentValue != 0.0f)
                    {
                        boundsReachedByComponent.put(
                                event.sourceComponent, Boolean.TRUE);
                        qualifyingValueByComponent.put(
                                event.sourceComponent, event.currentValue);
                        winner = event;
                    }
                }
                else
                {
                    // Other kind of axis, non-relative.  Force these to hit
                    // their max range in order to win.
                    if (!boundsHit)
                    {
                        // Wait for value to hit 1.0, then return to 0.
                        if (event.currentValue == 1.0f || event.currentValue == -1.0f)
                        {
                            boundsReachedByComponent.put(
                                    event.sourceComponent, Boolean.TRUE);
                            qualifyingValueByComponent.put(
                                    event.sourceComponent, event.currentValue);
                        }
                    }
                    else
                    {
                        // Value has already hit +/- 1.0.  Wait for it to return to 0f.
                        if (event.currentValue == 0.0f)
                        {
                            // Winner!
                            winner = event;
                        }
                    }
                }
            }
            else
            {
                throw new RuntimeException("Unsupported component type: "
                        + event.sourceComponentType);
            }

            // If a winner has been declared, notify any listeners that are
            // waiting.
            if (winner != null)
            {
                float qualifyingValue =
                    qualifyingValueByComponent.get(event.sourceComponent);
                winner = new ComponentEvent(
                        event.sourceController, event.sourceComponent,
                        event.userDefinedComponentId,
                        event.currentValue,
                        event.currentValueId,
                        qualifyingValue,
                        config.getConfiguration(event.sourceComponent)
                            .getValueId(qualifyingValue));
                latch.countDown();
            }
        }
    }

    /**
     * Can perform calibration on a component.
     * 
     * @author Andrew Hayden
     */
    private final class CalibrationHelper
    implements ComponentPollingListener
    {
        /**
         * Calibration results.
         */
        final CalibrationResults results = new CalibrationResults(controller);

        /**
         * Whether or not calibration is running.
         */
        private volatile boolean running = false;

        /**
         * Constructs a new calibration listener.
         */
        CalibrationHelper()
        {
            // Nothing yet...
        }

        @Override
        public final void componentPolled(final ComponentEvent event)
        {
            // Don't update any more if we've been asked to stop.
            if (!running || event.sourceComponent == null
                    || !eligibleComponents.contains(event.sourceComponent))
            {
                return;
            }

            boolean updated =
                results.processValue(event.sourceComponent, event.currentValue);
            if (updated)
            {
                final Range newRange = new Range(
                        results.getRange(event.sourceComponent));

                ControllerManager.eventDispatcher.submit(new LoggingRunnable(){
                    @Override
                    protected void runInternal()
                    {
                        for (CalibrationListener listener : calibrationListeners)
                        {
                            listener.calibrationResultsUpdated(
                                    controller, event.sourceComponent, newRange);
                        }
                    }
                });
            }
        }

        /**
         * Starts calibration.
         */
        final void start()
        {
            running = true;
        }

        /**
         * Halts calibration.
         */
        final void stop()
        {
            running = false;
        }
    }
}