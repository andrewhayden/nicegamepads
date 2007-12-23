package org.nicegamepads;

import java.util.List;

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
        ControllerUtils.loadGlobalDeadZones(controller, config, true, -0.1f, 0.1f);
        System.out.println(config);
        ControllerConfigurator configurator =
            new ControllerConfigurator(controller, config, true);
        
        ComponentEvent event = null;
        try
        {
            while(true)
            {
                event = configurator.identifyComponent();
                System.out.println("Component identified: " + event);
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        ControllerManager.shutdown();
    }
}
