package org.nicegamepads;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigurationPersistenceTest
{
    public final static void main(String[] args) throws IOException
    {
        ControllerManager.initialize();
        List<NiceController> gamepads = NiceController.getAllControllers();

        NiceController controller = gamepads.get(0);
        System.out.println("gamepad: " + controller.getDeclaredName() + "; fingerprint=" + controller.getFingerprint());

        controller.setAllAnalogDeadZones(-0.1f, 0.1f);
        controller.setAllAnalogGranularities(0.1f);
        ControllerConfiguration config = controller.getConfigurationLive();
        System.out.println(config);

        ControllerConfigurator configurator =
            new ControllerConfigurator(controller, config);
        
        ControlEvent event = null;
        Set<NiceControl> identifiedComponents = new HashSet<NiceControl>();
        try
        {
            for (int buttonIndex = 0; buttonIndex < 5; buttonIndex++)
            {
                System.out.println("Identify a new button...");
                event = configurator.identifyControl(NiceControlType.DISCRETE_INPUT, identifiedComponents);
                System.out.println("Component identified: " + event);
                ControlConfiguration componentConfig =
                    config.getConfiguration(event.sourceControl);
                componentConfig.setValueId(event.previousValue, buttonIndex);
                componentConfig.setUserDefinedId(buttonIndex);
                System.out.println("Bound value " + event.previousValue + " to user defined id " + buttonIndex);
                identifiedComponents.add(event.sourceControl);
            }

            for (int axisIndex = 5; axisIndex < 7; axisIndex++)
            {
                System.out.println("Identify a new axis...");
                event = configurator.identifyControl(NiceControlType.CONTINUOUS_INPUT, identifiedComponents);
                System.out.println("Component identified: " + event);
                ControlConfiguration componentConfig =
                    config.getConfiguration(event.sourceControl);
                componentConfig.setValueId(event.previousValue, axisIndex);
                componentConfig.setUserDefinedId(axisIndex);
                System.out.println("Bound value " + event.previousValue + " to user defined id " + axisIndex);
                identifiedComponents.add(event.sourceControl);
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        // Set granularity.
        System.out.println("Saving configuration using default namespace.");
        File saved = ConfigurationManager.saveConfigurationByType(config);
        System.out.println("Saved to: " + saved);

        System.out.println("Loading configuration from disk:");
        config = ConfigurationManager.loadConfigurationByType(controller);
        System.out.println(config);

        ControllerPoller poller = ControllerPoller.getInstance(controller);
        
        //final ControllerConfiguration staticConfig = config;
        ControlActivationListener activationListener =
            new ControlActivationListener(){

            @Override
            public void controlActivated(ControlEvent event)
            {
                if (event.userDefinedControlId != Integer.MIN_VALUE)
                {
                    System.out.println("Component id " + event.userDefinedControlId + " activated");
                }
                else
                {
                    System.out.println("unbound component activated.");
                }
            }

            @Override
            public void controlDeactivated(ControlEvent event)
            {
                if (event.userDefinedControlId != Integer.MIN_VALUE)
                {
                    System.out.println("Component id " + event.userDefinedControlId + " deactivated");
                }
                else
                {
                    System.out.println("unbound component deactivated.");
                }
            }
        };

        ControlChangeListener changeListener = new ControlChangeListener(){
            @Override
            public void valueChanged(ControlEvent event)
            {
                if (event.userDefinedControlId == 5 || event.userDefinedControlId == 6)
                {
                    // An axis we identified.
                    System.out.println("Axis id " + event.userDefinedControlId + " achieved new value: " + event.currentValue);
                }
                else
                {
                    System.out.println("Unbound axis " + event.userDefinedControlId + " achieved new value: " + event.currentValue);
                }
            }
        };

        ControlPollingListener pollingListener = new ControlPollingListener(){
            @Override
            public void controlPolled(ControlEvent event)
            {
                if (event.userDefinedControlId == 6)
                {
                    System.out.println("Component 6 has value: " + event.currentValue);
                }
            }
        };

        poller.addControlChangeListener(changeListener);
        poller.addControlActivationListener(activationListener);

        // Wait forever...
        System.out.println("Waiting for input.");
        //ControllerManager.shutdown();
    }
}
