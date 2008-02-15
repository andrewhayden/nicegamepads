package org.nicegamepads;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComponentIdentificationTest
{
    public final static void main(String[] args)
    {
        ControllerManager.initialize();
        List<NiceController> gamepads = NiceController.getAllControllers();
        for (NiceController controller : gamepads)
        {
            System.out.println("gamepad: " + controller.getDeclaredName() + "; fingerprint=" + controller.getFingerprint());
        }

        NiceController controller = gamepads.get(0);
        ControllerConfiguration config = new ControllerConfiguration(controller);
        //ControllerUtils.loadDeadZoneDefaults(controller, config, true);
        controller.setAnalogDeadZones(-0.1f, 0.1f);
        System.out.println(config);
        ControllerConfigurator configurator =
            new ControllerConfigurator(controller, config);
        
        ControlEvent event = null;
        Set<NiceControl> identifiedComponents = new HashSet<NiceControl>();
        try
        {
            for (int buttonIndex = 0; buttonIndex < 5; buttonIndex++)
            {
                System.out.println("Identify a new discrete input...");
                event = configurator.identifyControl(
                        NiceControlType.DISCRETE_INPUT, identifiedComponents);
                System.out.println("Component identified: " + event);
                identifiedComponents.add(event.sourceControl);
            }

            for (int axisIndex = 0; axisIndex < 2; axisIndex++)
            {
                System.out.println("Identify a new continuous input...");
                event = configurator.identifyControl(
                        NiceControlType.CONTINUOUS_INPUT, identifiedComponents);
                System.out.println("Component identified: " + event);
                identifiedComponents.add(event.sourceControl);
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        ControllerManager.shutdown();
    }
}
