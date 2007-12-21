package org.nicegamepads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Main entry point into NiceGamepads.
 * <p>
 * 
 * @author Andrew Hayden
 */
public final class ControllerManager
{
    /**
     * The one and only event dispatcher for controller polling.
     */
    final static ExecutorService eventDispatcher =
        Executors.newSingleThreadExecutor();

    /**
     * Private constructor discourages unwanted instantiation.
     */
    private ControllerManager()
    {
        // Private constructor discourages unwanted instantiation.
    }
}