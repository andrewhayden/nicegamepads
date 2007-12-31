package org.nicegamepads;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        ControllerUtils.loadGlobalDeadZones(controller, config, true, -0.1f, 0.1f);
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
                componentConfig.setValueId(event.currentValue, buttonIndex);
                System.out.println("Bound value " + event.currentValue + " to user defined id " + buttonIndex);
                identifiedComponents.add(event.sourceComponent);
            }

            for (int axisIndex = 5; axisIndex < 7; axisIndex++)
            {
                System.out.println("Identify a new axis...");
                event = configurator.identifyComponent(ComponentType.AXIS, identifiedComponents);
                System.out.println("Component identified: " + event);
                ComponentConfiguration componentConfig =
                    config.getConfigurationDeep(event.sourceComponent);
                componentConfig.setValueId(event.currentValue, axisIndex);
                System.out.println("Bound value " + event.currentValue + " to user defined id " + axisIndex);
                identifiedComponents.add(event.sourceComponent);
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        ControllerManager.shutdown();

        System.out.println("Saving configuration using default namespace.");
        File saved = ConfigurationManager.saveConfigurationByType(config);
        System.out.println("Saved to: " + saved);
    }
}
