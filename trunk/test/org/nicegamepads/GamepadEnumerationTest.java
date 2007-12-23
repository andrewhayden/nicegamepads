package org.nicegamepads;

import java.util.List;

import net.java.games.input.Controller;

public class GamepadEnumerationTest
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
        ControllerManager.shutdown();
    }
}
