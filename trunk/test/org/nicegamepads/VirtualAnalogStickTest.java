package org.nicegamepads;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class VirtualAnalogStickTest
{
    public final static void main(String[] args)
    {
        ControllerManager.initialize();
        List<NiceController> gamepads = NiceController.getAllControllers();

        NiceController controller = gamepads.get(0);
        System.out.println("gamepad: " + controller.getDeclaredName() + "; fingerprint=" + controller.getFingerprint());

        ControllerConfiguration config = new ControllerConfiguration(controller);
        controller.setAnalogDeadZones(-0.1f, 0.1f);
        controller.setAnalogGranularities(0.1f);
        System.out.println(config);

        ControllerConfigurator configurator =
            new ControllerConfigurator(controller, config);
        
        ControlEvent event = null;
        Set<NiceControl> identifiedControls = new HashSet<NiceControl>();
        NiceControl eastWest = null;
        HorizontalOrientation eastWestOrientation = null;
        NiceControl northSouth = null;
        VerticalOrientation northSouthOrientation = null;
        try
        {
            ControlConfiguration controlConfig = null;

            // East-west
            System.out.println("Identify east-west axis by pushing east...");
            event = configurator.identifyControl(NiceControlType.CONTINUOUS_INPUT, identifiedControls);
            System.out.println("Control identified: " + event);
            controlConfig = config.getConfigurationDeep(event.sourceControl);
            controlConfig.setUserDefinedId(0);
            identifiedControls.add(event.sourceControl);
            eastWest = event.sourceControl;
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
            event = configurator.identifyControl(NiceControlType.CONTINUOUS_INPUT, identifiedControls);
            System.out.println("Control identified: " + event);
            controlConfig = config.getConfigurationDeep(event.sourceControl);
            controlConfig.setUserDefinedId(1);
            identifiedControls.add(event.sourceControl);
            northSouth = event.sourceControl;
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

        ControllerPoller poller = new ControllerPoller(config);
        
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
