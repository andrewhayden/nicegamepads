package org.nicegamepads;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.java.games.input.Component;
import net.java.games.input.Controller;

public class ComponentIdentificationTest
{
    public final static void main(String[] args)
    {
        ControllerManager.initialize();
        List<Controller> gamepads = ControllerUtils.getAllGamepads(false);
        for (Controller c : gamepads)
        {
            System.out.println("gamepad: " + c.getName() + "; hash=" + ControllerUtils.generateTypeCode(c));
            System.out.println("on port: " + c.getPortNumber() + " (port type=" + c.getPortType() + ")");
        }

        Controller controller = gamepads.get(0);
        ControllerConfiguration config = new ControllerConfiguration(controller);
        //ControllerUtils.loadDeadZoneDefaults(controller, config, true);
        ControllerUtils.setAnalogDeadZones(controller, config, true, -0.1f, 0.1f);
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
                event = configurator.identifyComponent(
                        ComponentType.BUTTON, identifiedComponents);
                System.out.println("Component identified: " + event);
                identifiedComponents.add(event.sourceComponent);
            }

            for (int axisIndex = 0; axisIndex < 2; axisIndex++)
            {
                System.out.println("Identify a new axis...");
                event = configurator.identifyComponent(
                        ComponentType.AXIS, identifiedComponents);
                System.out.println("Component identified: " + event);
                identifiedComponents.add(event.sourceComponent);
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        ControllerManager.shutdown();
    }
}
