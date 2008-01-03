package org.nicegamepads;

/**
 * Represents an error condition where there is an attempt to access a
 * component that does not exist.
 * 
 * @author Andrew Hayden
 */
public class NoSuchComponentException extends RuntimeException
{
    /**
     * Constructs a new exception.
     */
    public NoSuchComponentException()
    {
        super();
    }

    /**
     * @param message
     * @param cause
     */
    public NoSuchComponentException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * @param message
     */
    public NoSuchComponentException(String message)
    {
        super(message);
    }

    /**
     * @param cause
     */
    public NoSuchComponentException(Throwable cause)
    {
        super(cause);
    }
}