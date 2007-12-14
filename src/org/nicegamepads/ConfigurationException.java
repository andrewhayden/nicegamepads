package org.nicegamepads;

/**
 * An exception representing a configuration error.
 *
 * @author Andrew Hayden
 */
public class ConfigurationException extends Exception
{
    /**
     * Default constructor.
     */
    public ConfigurationException()
    {
        super();
    }

    /**
     * @param message
     * @param cause
     */
    public ConfigurationException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * @param message
     */
    public ConfigurationException(String message)
    {
        super(message);
    }

    /**
     * @param cause
     */
    public ConfigurationException(Throwable cause)
    {
        super(cause);
    }
}