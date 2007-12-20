package org.nicegamepads;

import java.util.List;

import net.java.games.input.Controller;

public class GamepadEnumerationTest
{
    public final static void main(String[] args)
    {
        //List<Controller> gamepads = ControllerUtils.getAllGamepads(false);
        List<Controller> gamepads = ControllerUtils.getAllControllers();
        for (Controller c : gamepads)
        {
            System.out.println("gamepad: " + c.getName() + "; hash=" + ControllerUtils.generateTypeCode(c));
            System.out.println("on port: " + c.getPortNumber() + " (port type=" + c.getPortType() + ")");
        }
    }
}
