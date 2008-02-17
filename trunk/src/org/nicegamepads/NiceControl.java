package org.nicegamepads;

import java.util.HashMap;
import java.util.Map;

import net.java.games.input.Component;

public class NiceControl
{
    /**
     * Map of all NiceComponent objects by Component instance.
     */
    private final static Map<Component, NiceControl> wrappersByControl
        = new HashMap<Component, NiceControl>();

    private final NiceControlType controlType;

    /**
     * The control being wrapped.
     */
    private final Component jinputComponent;

    /**
     * The controller that owns this control.
     */
    private final NiceController controller;

    /**
     * Fingerprint for this control.
     */
    private final int fingerprint;

    /**
     * Constructs a new wrapper for the specific control.
     * <p>
     * All NiceController instances share a static cache of objects that is
     * used to provide and enforce consistently safe access to the underyling
     * physical device.
     * 
     * @param jinputComponent the control to wrap
     * @param controller the controller that contains this control
     */
    private NiceControl(Component jinputComponent, NiceController controller)
    {
        this.jinputComponent = jinputComponent;
        this.controller = controller;
        // FIXME: Need to set good control type
        this.controlType = NiceControlType.CONTINUOUS_INPUT;
        this.fingerprint = generateFingerprint();
    }

    /**
     * Returns the wrapper for the specified control; there is exactly
     * one wrapper per physical device, and no more.
     * 
     * @param control the device to be wrapped
     * @return the singleton wrapper for the physical device
     */
    final static NiceControl getInstance(Component control,
            NiceController owner)
    {
        synchronized(wrappersByControl)
        {
            NiceControl instance = wrappersByControl.get(control);
            if (instance == null)
            {
                instance = new NiceControl(control, owner);
                wrappersByControl.put(control, instance);
            }
            return instance;
        }
    }

    public final String getDeclaredName()
    {
        return jinputComponent.getName();
    }

    public final int getFingerprint()
    {
        return fingerprint;
    }

    /**
     * Generates a fingerprint for this kind of control.
     * <p>
     * The fingerprint should be the same every time the same control is
     * present, even between runs, provided that there are no changes to
     * drivers or jinput libraries.  It should also be the same regardless
     * of which individual <em>phsyical controller</em> is plugged in so
     * long as it is identical to others of its type.  For example, if you
     * plug in one XBOX360 controller, its buttons should have the same
     * fingerprints as the buttons of every other XBOX360 controller
     * of the same hardware revision.
     * <p>
     * Note that this is <em>not</em> a hashcode.  It does <em>not</em>
     * uniquely identify an individual piece of hardware.
     * 
     * @return the fingerprint for this kind of control
     */
    private final int generateFingerprint()
    {
        int result = 39;
        String name = getDeclaredName();
        result += 39 * (name == null? 0 : name.hashCode());
        result += 39 * controlType.name().hashCode();
        return result;
    }

    /**
     * Load the default dead zones for the control into the live configuration.
     * <p>
     * This method takes effect immediately.
     */
    public final void loadDeadZoneDefaults()
    {
        ControllerConfiguration ownerConfig = controller.getConfigurationLive();
        ownerConfig.lockConfiguration();
        ControlConfiguration config = ownerConfig.getConfiguration(this);
        float defaultDeadZone = jinputComponent.getDeadZone();
        if (!Float.isNaN(defaultDeadZone)
                && !Float.isInfinite(defaultDeadZone))
        {
            // Valid float found.
            // We only care about the absolute value; kill negatives.
            defaultDeadZone = Math.abs(defaultDeadZone);
            // Sanity check, clamp to range [0,1]
            defaultDeadZone = Math.min(defaultDeadZone, 1.0f);
            // Configure
            config.setDeadZoneBounds(
                -1f * defaultDeadZone, defaultDeadZone);
        }
        else
        {
            config.setDeadZoneBounds(Float.NaN, Float.NaN);
        }
        ownerConfig.unlockConfiguration();
    }

    /**
     * Returns the type of this control.
     * 
     * @return the type of this control.
     */
    public final NiceControlType getControlType()
    {
        return controlType;
    }

    /**
     * Returns the controller that contains this control.
     * 
     * @return the controller that contains this control.
     */
    public final NiceController getController()
    {
        return controller;
    }
}