package org.nicegamepads;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.java.games.input.Component;
import net.java.games.input.Controller;

public class ConfigurationPersistenceTest
{
    public final static void main(String[] args) throws IOException
    {
        ControllerManager.initialize();
        List<Controller> gamepads = ControllerUtils.getAllGamepads(false);

        Controller controller = gamepads.get(0);
        System.out.println("gamepad: " + controller.getName() + "; hash=" + ControllerUtils.generateTypeCode(controller));
        System.out.println("on port: " + controller.getPortNumber() + " (port type=" + controller.getPortType() + ")");

        ControllerConfiguration config = new ControllerConfiguration(controller);
        ControllerUtils.setAnalogDeadZones(controller, config, true, -0.1f, 0.1f);
        ControllerUtils.setAnalogGranularities(controller, config, true, 0.1f);
        System.out.println(config);

        ControllerConfigurator configurator =
            new ControllerConfigurator(controller, config, true);
        
        ComponentEvent event = null;
        Set<Component> identifiedComponents = new HashSet<Component>();
        try
        {
            for (int buttonIndex = 0; buttonIndex < 5; buttonIndex++)
            {
                System.out.println("Identify a new button...");
                event = configurator.identifyComponent(ComponentType.BUTTON, identifiedComponents);
                System.out.println("Component identified: " + event);
                ComponentConfiguration componentConfig =
                    config.getConfigurationDeep(event.sourceComponent);
                componentConfig.setValueId(event.previousValue, buttonIndex);
                componentConfig.setUserDefinedId(buttonIndex);
                System.out.println("Bound value " + event.previousValue + " to user defined id " + buttonIndex);
                identifiedComponents.add(event.sourceComponent);
            }

            for (int axisIndex = 5; axisIndex < 7; axisIndex++)
            {
                System.out.println("Identify a new axis...");
                event = configurator.identifyComponent(ComponentType.AXIS, identifiedComponents);
                System.out.println("Component identified: " + event);
                ComponentConfiguration componentConfig =
                    config.getConfigurationDeep(event.sourceComponent);
                componentConfig.setValueId(event.previousValue, axisIndex);
                componentConfig.setUserDefinedId(axisIndex);
                System.out.println("Bound value " + event.previousValue + " to user defined id " + axisIndex);
                identifiedComponents.add(event.sourceComponent);
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

        ControllerPoller poller = new ControllerPoller(controller, true);
        poller.setConfiguration(config);
        
        //final ControllerConfiguration staticConfig = config;
        ComponentActivationListener activationListener =
            new ComponentActivationListener(){

            @Override
            public void componentActivated(ComponentEvent event)
            {
                if (event.userDefinedComponentId != Integer.MIN_VALUE)
                {
                    System.out.println("Component id " + event.userDefinedComponentId + " activated");
                }
                else
                {
                    System.out.println("unbound component activated.");
                }
            }

            @Override
            public void componentDeactivated(ComponentEvent event)
            {
                if (event.userDefinedComponentId != Integer.MIN_VALUE)
                {
                    System.out.println("Component id " + event.userDefinedComponentId + " deactivated");
                }
                else
                {
                    System.out.println("unbound component deactivated.");
                }
            }
        };

        ComponentChangeListener changeListener = new ComponentChangeListener(){
            @Override
            public void valueChanged(ComponentEvent event)
            {
                if (event.userDefinedComponentId == 5 || event.userDefinedComponentId == 6)
                {
                    // An axis we identified.
                    System.out.println("Axis id " + event.userDefinedComponentId + " achieved new value: " + event.currentValue);
                }
                else
                {
                    System.out.println("Unbound axis " + event.userDefinedComponentId + " achieved new value: " + event.currentValue);
                }
            }
        };

        ComponentPollingListener pollingListener = new ComponentPollingListener(){
            @Override
            public void componentPolled(ComponentEvent event)
            {
                if (event.userDefinedComponentId == 6)
                {
                    System.out.println("Component 6 has value: " + event.currentValue);
                }
            }
        };

        poller.addComponentChangeListener(changeListener);
        poller.addComponentActivationListener(activationListener);
        //poller.addComponentPollingListener(pollingListener);
        poller.startPolling(10, TimeUnit.MILLISECONDS);

        // Wait forever...
        System.out.println("Waiting for input.");
        //ControllerManager.shutdown();
    }
}
