package org.nicegamepads;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.java.games.input.Component;
import net.java.games.input.Controller;

public class VirtualAnalogStickTest
{
    public final static void main(String[] args)
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
        Component eastWest = null;
        HorizontalOrientation eastWestOrientation = null;
        Component northSouth = null;
        VerticalOrientation northSouthOrientation = null;
        try
        {
            ComponentConfiguration componentConfig = null;

            // East-west
            System.out.println("Identify east-west axis by pushing east...");
            event = configurator.identifyComponent(ComponentType.AXIS, identifiedComponents);
            System.out.println("Component identified: " + event);
            componentConfig = config.getConfigurationDeep(event.sourceComponent);
            componentConfig.setUserDefinedId(0);
            identifiedComponents.add(event.sourceComponent);
            eastWest = event.sourceComponent;
            if (event.previousValue > 0)
            {
                // Normal orientation
                eastWestOrientation = HorizontalOrientation.EAST_POSITIVE;
            }
            else
            {
                eastWestOrientation = HorizontalOrientation.WEST_POSITIVE;
            }

            // North-south
            System.out.println("Identify north-south axis by pushing south ...");
            event = configurator.identifyComponent(ComponentType.AXIS, identifiedComponents);
            System.out.println("Component identified: " + event);
            componentConfig = config.getConfigurationDeep(event.sourceComponent);
            componentConfig.setUserDefinedId(1);
            identifiedComponents.add(event.sourceComponent);
            northSouth = event.sourceComponent;
            if (event.previousValue > 0)
            {
                // Normal orientation
                northSouthOrientation = VerticalOrientation.SOUTH_POSITIVE;
            }
            else
            {
                northSouthOrientation = VerticalOrientation.NORTH_POSITIVE;
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        ControllerPoller poller = new ControllerPoller(config, true);
        
        //final ControllerConfiguration staticConfig = config;
        final VirtualAnalogStick virtualStick = new VirtualAnalogStick(
                VirtualAnalogStick.PhysicalConstraints.CIRCULAR,
                eastWest, northSouth,
                eastWestOrientation, northSouthOrientation);

        final BoundedVector vector = new BoundedVector();

        poller.addControllerPollingListener(new ControllerPollingListener(){
            @Override
            public void controllerPolled(ControllerState controllerState)
            {
                virtualStick.process(controllerState, vector);
                System.out.println("Virtual stick: degrees=" + vector.directionCompassDegrees + ", magnitude=" + vector.magnitude);
            }
        });

        poller.startPolling(10, TimeUnit.MILLISECONDS);

        // Wait forever...
        System.out.println("Waiting for input.");
    }
}
