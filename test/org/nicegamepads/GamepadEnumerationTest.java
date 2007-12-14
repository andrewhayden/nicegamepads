package org.nicegamepads;

import java.util.List;

import net.java.games.input.Controller;

public class GamepadEnumerationTest
{
    public final static void main(String[] args)
    {
        List<Controller> gamepads = ControllerUtils.getAllGamepads(false);
        for (Controller c : gamepads)
        {
            System.out.println("gamepad: " + c.getName() + "; hash=" + ControllerUtils.generateTypeCode(c));
        }
    }
}
