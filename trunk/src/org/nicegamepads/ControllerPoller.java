package org.nicegamepads;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.java.games.input.Controller;

/**
 * Abstracts the polling of a single processor.
 * 
 * @author Andrew Hayden
 */
public class ControllerPoller
{
    /**
     * The controller to be polled by this object.
     */
    private final Controller controller;

    /**
     * The controller state used to store the state of the controller and
     * all of its components.
     */
    private final ControllerState controllerState;

    /**
     * The configuration for this poller.
     */
    private volatile ControllerConfiguration volatileConfiguration;

    /**
     * Holds cache of granularity calculations.
     */
    private final static Map<Float, float[]> granularityBinsByGranularity =
        new ConcurrentHashMap<Float, float[]>();

    /**
     * Listeners.
     */
    private final List<ComponentActivationListener> activationListeners =
        new CopyOnWriteArrayList<ComponentActivationListener>();

    /**
     * Listeners.
     */
    private final List<ComponentChangeListener> changeListeners =
        new CopyOnWriteArrayList<ComponentChangeListener>();

    /**
     * Listeners.
     */
    private final List<ComponentPollingListener> pollingListeners =
        new CopyOnWriteArrayList<ComponentPollingListener>();

    /**
     * Polling service that handles polling of the controller.
     */
    private final static ScheduledExecutorService pollingService =
        Executors.newSingleThreadScheduledExecutor();

    /**
     * Used to call the {@link #poll()} method periodically.
     */
    private final PollingInvoker pollingInvoker;

    /**
     * Currently-scheduled polling task, if any.
     */
    private ScheduledFuture<?> pollingTask = null;

    /**
     * Constructs a new poller for the specified controller, optionally
     * descending into all subcontrollers.
     * <p>
     * All of the components found within this controller (and, if requested,
     * all of its subcontrollers recursively expanded) can be polled
     * conveniently with this object.
     * 
     * @param controller the controller to be polled
     * @param deep whether or not to descend recursively into any and all
     * subcontrollers or not
     */
    public ControllerPoller(Controller controller, boolean deep)
    {
        this.controller = controller;
        this.controllerState = new ControllerState(controller, deep);

        // This call is important!  Don't remove it!
        // It has a side effect
        ControllerUtils.getComponents(controller, true);

        pollingInvoker = new PollingInvoker(this);
    }

    /**
     * Requests that <strong>all</strong> polling cease in the near future.
     * Events that are currently enqueued are allowed to start and complete.
     */
    public final static void shutdownAllPolling()
    {
        pollingService.shutdown();
    }

    /**
     * Requests that <strong>all</strong> polling cease as soon as possible.
     * All currently-executing events are asked to halt (via interrupt)
     * and all enqueued events are dropped on the floor.
     */
    public final static void shutdownAllPollingNow()
    {
        pollingService.shutdownNow();
    }

    /**
     * Waits for all polling to cease.
     * 
     * @param timeout the maximum amount of time to wait for termination
     * @param unit the unit of time to wait for
     * @return <code>true</code> if all polling has ceased when this method
     * returns; otherwise, <code>false</code> if the timeout expires first
     * @throws InterruptedException if interrupted while waiting
     */
    public final static boolean awaitTermination(long timeout, TimeUnit unit)
    throws InterruptedException
    {
        return pollingService.awaitTermination(timeout, unit);
    }

    /**
     * Retrieves the bins appropriate for the specified granularity.
     * <p>
     * If the bins haven't been cached, they are cached before being returned
     * so that later access will be fast.
     * 
     * @param granularity the granularity to use to generate the bins
     * @return an array of bins, sorted in natural order from least to
     * greatest
     */
    private final static float[] getGranularityBins(float granularity)
    {
        float[] bins = granularityBinsByGranularity.get(granularity);
        if (bins != null)
        {
            // Cached.  Return this immediately.
            return bins;
        }

        List<Float> listing = new LinkedList<Float>();
        int counter = 1;
        float currentValue = 0f;
        while (currentValue > -1.0f)
        {
            currentValue = 0f - (((float) counter) * granularity);
            listing.add(0, currentValue);
            counter++;
        }
        listing.add(0f);
        counter = 1;
        currentValue = 0f;
        while (currentValue < 1.0f)
        {
            currentValue = ((float) counter) * granularity;
            listing.add(currentValue);
            counter++;
        }

        // List complete!
        granularityBinsByGranularity.put(granularity, bins);
        return bins;
    }

    /**
     * Returns an immutable version of the current configuation.
     * <p>
     * Subsequent changes to the configuration of this poller are not
     * reflected in the returned object.  The returned object cannot be
     * modified.  If you need a mutable configuration object, use the
     * {@link #getConfiguration()} method instead.
     * <p>
     * This method is intended primarily for consumers who need to query
     * the configuration quickly and inexpensively, and aren't interested
     * in making changes to it.
     * 
     * @return an immutable version of the current configuration
     */
    public final ControllerConfiguration getImmutableConfiguration()
    {
        return volatileConfiguration;
    }

    /**
     * Returns the controller that this poller polls for.
     * 
     * @return the controller that this poller polls for
     */
    public final Controller getController()
    {
        return controller;
    }

    /**
     * Returns an independent copy of the current configuration.
     * <p>
     * Unlike {@link #getImmutableConfiguration()}, the returned object
     * is mutable.  Note that the returned object is an independent copy
     * of the currnet configuration.
     * 
     * @return
     */
    public final ControllerConfiguration getConfiguration()
    {
        return new ControllerConfiguration(volatileConfiguration);
    }

    /**
     * Sets a new configuration which will be used for the next polling
     * operation.
     * <p>
     * An independent copy of the speciifed configuration is made and used
     * for all polling operations that begin after this method completes.
     * <p>
     * This method is threadsafe.
     * 
     * @param configuration the configuration to use
     */
    public final void setConfiguration(ControllerConfiguration configuration)
    {
        // We will store an immutable reference as we should never, EVER
        // change the configuration ourselves.
        this.volatileConfiguration =
            ConfigurationUtils.immutableControllerConfiguration(configuration);
    }

    /**
     * Starts or resumes polling the controller associated with this poller.
     * <p>
     * If polling is already running, cancels the next polling interval and
     * reschedules polling at the specified interval.
     * <p>
     * In any case, polling will start after the specified interval has
     * passed.
     * 
     * @param interval the interval at which to poll
     * @param unit the time unit for the interval
     */
    public final void startPolling(long interval, TimeUnit unit)
    {
        synchronized(pollingInvoker)
        {
            if (pollingTask != null)
            {
                pollingTask.cancel(false);
            }
            pollingTask = pollingService.scheduleAtFixedRate(
                    pollingInvoker, interval, interval, unit);
        }
    }

    /**
     * Cancels any outstanding polling schedules immediately and stops all
     * future polling.  No further polling will be performed.
     * <p>
     * Note that this method may result in one last polling event executing
     * if the request to stop overlaps with such an event.  If you need
     * to guarantee that polling has terminated by the time this call
     * returns, use {@link #stopPollingAndWait(long, TimeUnit)} instead.
     */
    public final void stopPolling()
    {
        synchronized(pollingInvoker)
        {
            if (pollingTask != null)
            {
                pollingTask.cancel(false);
            }
            pollingTask = null;
        }
    }

    /**
     * Cancels any outstanding polling schedules immediately waits until
     * the currently-executing polling process, if any, has completed.
     * After this method has returned, it is guaranteed that no further
     * polling events will be enqueued for dispatch (any unprocessed
     * events will still be fired).
     */
    public final void stopPollingAndWait(long interval, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException
    {
        synchronized(pollingInvoker)
        {
            try
            {
                if (pollingTask != null)
                {
                    pollingTask.cancel(false);
                    pollingTask.get(interval, unit);
                }
            }
            finally
            {
                pollingTask = null;
            }
        }
    }

    /**
     * Forces this controller to poll all of its components immediately.
     * <p>
     * Polling will dispatch events for the components as necessary.
     * 
     * @throws IllegalStateException if no configuration has been set for
     * this poller
     */
    private final void poll()
    {
        // Start by obtaining a copy of the current configuration.  We will
        // use only this copy for our entire method lifetime.
        // Since the source is volatile we are guaranteed that we are getting
        // the latest copy here.
        final ControllerConfiguration config = volatileConfiguration;
        if (config == null)
        {
            throw new IllegalStateException("Null configuration.");
        }

        ComponentConfiguration componentConfig = null;
        final long now = System.currentTimeMillis();

        // Poll the controller
        boolean controllerOk = controller.poll();
        if (!controllerOk)
        {
            // FIXME: raise event!
            stopPolling();
        }

        // Process each component.
        for (ComponentState state : controllerState.componentStates)
        {
            // Look up configuration for this component
            componentConfig = config.getConfigurationDeep(state.component);
            // Poll the value
            float polledValue = state.component.getPollData();
            // Transform according to configuration rules
            polledValue = transform(polledValue, componentConfig, state.componentType);
            // Get any user ID bound to this value
            boolean forceFireTurboEvent = false;

            switch (state.componentType)
            {
                case BUTTON:
                case KEY:
                    state.newValue(polledValue, now, polledValue == 1f);
                    // Check for turbo stuff
                    if (componentConfig.isTurboEnabled && polledValue == 1f)
                    {
                        if (componentConfig.turboDelayMillis == 0)
                        {
                            // If button is pressed, force an event.
                            forceFireTurboEvent = true;
                        }
                        else if (state.lastTurboTimerStart > 0)
                        {
                            // There is a specific delay for turbo and the
                            // timer is running.  Has enough time gone by?
                            if (now - state.lastTurboTimerStart >=
                                componentConfig.turboDelayMillis)
                            {
                                // Yes.
                                forceFireTurboEvent = true;
                            }
                        }
                    }
                    break;
                case AXIS:
                    state.newValue(polledValue, now, false);
                    break;
                default:
                    throw new RuntimeException(
                            "Unsupported component type: " + state.componentType);
            }

            ComponentEvent event = makeEvent(state, componentConfig);
            // Figure out which events need to be fired.

            // Start with activation/deactivation events:
            if (event.previousValueId != Integer.MIN_VALUE
                    && event.currentValueId != Integer.MIN_VALUE)
            {
                // Both values are bound.
                if (event.previousValueId != event.currentValueId)
                {
                    // Previous id deactivated.
                    dispatchComponentDeactivated(event);
                    // Current id activated
                    dispatchComponentActivated(event);
                }
                else
                {
                    // Previous id is still active.
                    if (forceFireTurboEvent)
                    {
                        // Turbo mode engaged.  Force an event to fire even
                        // though nothing has really changed.
                        dispatchComponentActivated(event);
                    }
                }
            }
            else if (event.previousValueId != Integer.MIN_VALUE)
            {
                // Previous value is bound but current value isn't.
                dispatchComponentDeactivated(event);
            }
            else if (event.currentValueId != Integer.MIN_VALUE)
            {
                // Current value is bound but previous value isn't.
                dispatchComponentActivated(event);
            }
            else
            {
                // Neither the current nor the previous value is bound
                // No-op (only thing that can happen is a change event,
                // which we don't test by ID since IDs aren't guaranteed
                // to be unique).
            }

            // Check for a value change and fire event if the value has changed
            if (event.previousValue != event.currentValue)
            {
                dispatchValueChanged(event);
            }

            // Fire generic polling event
            dispatchComponentPolled(event);
        }
    }

    /**
     * Applies any and all transforms to the polled value, as defined by
     * the configuration, and returns the result.
     * 
     * @param polledValue the value that was polled from the component
     * @param componentConfig the component configuration to consult
     * @param the type of component
     * @return the post-transform result for the value
     */
    private final float transform(float polledValue,
            ComponentConfiguration componentConfig, ComponentType componentType)
    {
        // STEP 1: Granularity
        // Get granularity bins and collapse.
        if (!Float.isNaN(componentConfig.granularity))
        {
            float[] bins = getGranularityBins(componentConfig.granularity);
            int index = Arrays.binarySearch(bins, polledValue);
            if (index < 0)
            {
                // Value isn't an exact match to any bin, so we must clamp
                // it to the bin boundary nearest to zero
                // Index returned by binary search is
                // (-1 * insertionPoint) - 1, such that an insertion point
                // of zero maps to -1, one maps to -2, and so on.
                // Start by getting 
                index = (index + 1) * -1;
                // Now we have zero mapping to 0, one to 1, two to 2...
                // If the value is less than zero, it has to move in a positive
                // direction, and therefore it must have 1 added to its index
                if (polledValue < 0)
                {
                    index++;
                }

                // Now we have the right bin for the value.
                polledValue = bins[index];
            }
        }

        // STEP 2: Dead zone
        if (!Float.isNaN(componentConfig.deadZoneLowerBound))
        {
            // We have a dead zone to consider.
            if (componentConfig.deadZoneLowerBound <= polledValue
                    && polledValue <= componentConfig.deadZoneUpperBound)
            {
                // Value is in the dead zone.  Set to zero.
                polledValue = 0f;
            }
        }

        // STEP 3: Inversion
        if (componentConfig.isInverted)
        {
            if (componentType == ComponentType.BUTTON)
            {
                // Reflect around 0.5f
                polledValue = 1f - polledValue;
            }
            else
            {
                // Reflect around 0f
                polledValue *= -1f;
            }
        }

        // STEP 4: Recentering
        if (!Float.isNaN(componentConfig.centerValueOverride)
                && componentConfig.centerValueOverride != 0f)
        {
            float positiveExpansion;
            float negativeExpansion;
            if (componentType == ComponentType.BUTTON)
            {
                
            }
            else
            {
                if (componentConfig.centerValueOverride > 0)
                {
                    positiveExpansion = 1f - componentConfig.centerValueOverride;
                    negativeExpansion = 1f + componentConfig.centerValueOverride;
                }
                else
                {
                    negativeExpansion = 1f - Math.abs(componentConfig.centerValueOverride);
                    positiveExpansion = 1f - componentConfig.centerValueOverride;
                }

                if (polledValue > 0)
                {
                    polledValue *= positiveExpansion;
                }
                else if (polledValue < 0)
                {
                    polledValue *= negativeExpansion;
                }
                else
                {
                    polledValue = componentConfig.centerValueOverride;
                }
            }
        }

        // STEP 5: Clamp to sane range
        // We don't expect to get out of this range because of any errors
        // in the code, but rather it is possible because of floating point
        // precision loss.
        if (polledValue < -1.0f)
        {
            polledValue = -1.0f;
        }
        else if (polledValue > 1.0f)
        {
            polledValue = 1.0f;
        }

        // All done!
        return polledValue;
    }

    /**
     * Adds a listener to this poller to be notified whenever
     * a component is activated or deactivated.
     * 
     * @param listener the listener to add.
     */
    public final void addComponentActivationListener(
            ComponentActivationListener listener)
    {
        activationListeners.add(listener);
    }

    /**
     * Removes the specified listener.
     * 
     * @param listener the listener to be removed
     */
    public final void removeComponentActivationListener(
            ComponentActivationListener listener)
    {
        activationListeners.remove(listener);
    }

    /**
     * Adds a listener to this poller to be notified whenever
     * a component's value changes.
     * 
     * @param listener the listener to add.
     */
    public final void addComponentChangeListener(
            ComponentChangeListener listener)
    {
        changeListeners.add(listener);
    }

    /**
     * Removes the specified listener.
     * 
     * @param listener the listener to be removed
     */
    public final void removeComponentChangeListener(
            ComponentChangeListener listener)
    {
        changeListeners.remove(listener);
    }

    /**
     * Adds a listener to this poller to be notified whenever
     * a component is polled.
     * 
     * @param listener the listener to add.
     */
    public final void addComponentPollingListener(
            ComponentPollingListener listener)
    {
        pollingListeners.add(listener);
    }

    /**
     * Removes the specified listener.
     * 
     * @param listener the listener to be removed
     */
    public final void removeComponentPollingListener(
            ComponentPollingListener listener)
    {
        pollingListeners.remove(listener);
    }

    /**
     * Makes an event from the specified state and configuration.
     * 
     * @param state the state to base the event on
     * @param config the config associated with the component
     * @return the event
     */
    private final static ComponentEvent makeEvent(
            ComponentState state, ComponentConfiguration config)
    {
        return new ComponentEvent(
                ControllerUtils.getCachedParentController(state.component),
                state.component, config.getUserDefinedId(),
                state.currentValue, config.getValueId(state.currentValue),
                state.lastValue, config.getValueId(state.lastValue));
    }

    /**
     * Dispatches a new "component activated" event to all registered
     * listeners.
     * 
     * @param event the event to be dispatched
     */
    private final void dispatchComponentActivated(final ComponentEvent event)
    {
        ControllerManager.eventDispatcher.submit(new LoggingRunnable(){
            @Override
            protected void runInternal()
            {
                for (ComponentActivationListener listener : activationListeners)
                {
                    listener.componentActivated(event);
                }
            }
        });
    }

    /**
     * Dispatches a new "component deactivated" event to all registered
     * listeners.
     * 
     * @param event the event to be dispatched
     */
    private final void dispatchComponentDeactivated(final ComponentEvent event)
    {
        ControllerManager.eventDispatcher.submit(new LoggingRunnable(){
            @Override
            protected void runInternal()
            {
                for (ComponentActivationListener listener : activationListeners)
                {
                    listener.componentDeactivated(event);
                }
            }
        });
    }

    /**
     * Dispatches a new "value changed" event to all registered
     * listeners.
     * 
     * @param event the event to be dispatched
     */
    private final void dispatchValueChanged(final ComponentEvent event)
    {
        ControllerManager.eventDispatcher.submit(new LoggingRunnable(){
            @Override
            protected void runInternal()
            {
                for (ComponentChangeListener listener : changeListeners)
                {
                    listener.valueChanged(event);
                }
            }
        });
    }

    /**
     * Dispatches a new "component polled" event to all registered
     * listeners.
     * 
     * @param event the event to be dispatched
     */
    private final void dispatchComponentPolled(final ComponentEvent event)
    {
        ControllerManager.eventDispatcher.submit(new LoggingRunnable(){
            @Override
            protected void runInternal()
            {
                for (ComponentPollingListener listener : pollingListeners)
                {
                    listener.componentPolled(event);
                }
            }
        });
    }

    /**
     * Utility class to invoke polling on a controller poller.
     * 
     * @author Andrew Hayden
     */
    private final static class PollingInvoker extends LoggingRunnable
    {
        /**
         * The poller to invoke polling on.
         */
        private final ControllerPoller poller;

        /**
         * Constructs a new invoker that will invoke polling on the specified
         * controller poller.
         * 
         * @param poller the poller to invoke polling on
         */
        PollingInvoker (ControllerPoller poller)
        {
            this.poller = poller;
        }

        @Override
        protected final void runInternal()
        {
            poller.poll();
        }
    }
}