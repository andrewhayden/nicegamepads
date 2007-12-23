package org.nicegamepads;

import java.util.List;

import org.nicegamepads.CalibrationResults.Range;

import net.java.games.input.Component;
import net.java.games.input.Controller;

public class ControllerCalibrationTest
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
        ControllerUtils.loadDeadZoneDefaults(controller, config, true);
        System.out.println(config);
        ControllerConfigurator configurator =
            new ControllerConfigurator(controller, config, true);

        configurator.addCalibrationListener(new CalibrationListener(){

            @Override
            public void calibrationResultsUpdated(Controller controller,
                    Component component, Range range)
            {
                System.out.println("Calibration updated for controller \""
                        + controller + "\", component \""
                        + component.getName() + "\": "
                        + range);
            }

            @Override
            public void calibrationStarted(Controller controller)
            {
                System.out.println("Calibration started for controller \""
                        + controller + "\"");
            }

            @Override
            public void calibrationStopped(Controller controller,
                    CalibrationResults results)
            {
                System.out.println("Calibration complete for controller \""
                        + controller + "\":");
                System.out.println(results);
            }
        });

        System.out.println("Asking calibration to start.");
        configurator.startCalibrating();
        try
        {
            System.out.println("Waiting for 30 seconds.");
            synchronized(configurator)
            {
                configurator.wait(30000L);
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        System.out.println("Asking calibration to stop.");
        configurator.stopCalibrating();
        ControllerManager.shutdown();
    }
}
