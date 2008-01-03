package org.nicegamepads;

/**
 * Container class that defines a true vector with both a directional
 * component as well as a magnitude component.
 * <p>
 * The values contained in this vector are always bounded, thus the
 * name "BoundedVector".  Specifically, the direction will always
 * be in the range [0, 360) and the magnitude will always be in the
 * range [0, {@link #maxMagnitude}].
 * 
 * @author Andrew Hayden
 */
public class BoundedVector
{
    /**
     * The direction, expressed as degrees in the range [0, 360) using
     * standard java values (0=east, 90=south, 180=west,
     * -90=north).
     */
    public float directionJavaDegrees = 0f;

    /**
     * The direction, expressed as degrees in the range [0, 360) using
     * standard magnetic compass values (0=north, 90=east, 180=south,
     * 270=west).
     */
    public float directionCompassDegrees = 0f;

    /**
     * The direction, expressed as radians in the range [-1 * pi, pi],
     * using standard java values (0=east, south=pi/2,
     * west=pi, north=-pi/2)
     */
    public float directionJavaRadians = 0f;

    /**
     * The direction, expressed as radians in the range [-1 * pi, pi],
     * using standard magnetic compass values (0=north, east=pi/2,
     * south=pi, west=3pi/2)
     */
    public float directionCompassRadians = 0;

    /**
     * The magnitude, expressed as percentage of maximum, in the range
     * [0,1].
     */
    public float magnitude = 0f;

    /**
     * For convenience, the easterly component of the overall magnitude.
     * This value is always normalized such that the maximum
     * eastwest-oriented magnitude corresponds to moving east and has
     * the value 1.0, while the minimum eastwest-oriented magnitude
     * corresponds to moving west and has the value -1.0.
     * <p>
     * This is primarily useful for clients that divide movement into
     * its horizontal and vertical components instead of considering it
     * as a true compass heading.
     */
    public float easterlyComponent = 0f;

    /**
     * For convenience, the southerly component of the overall magnitude.
     * This value is always normalized such that the maximum
     * northsouth-oriented magnitude corresponds to moving south and has
     * the value 1.0, while the minimum northsouth-oriented magnitude
     * corresponds to moving north and has the value -1.0.
     * <p>
     * This is primarily useful for clients that divide movement into
     * its horizontal and vertical components instead of considering it
     * as a true compass heading.
     */
    public float southerlyComponent = 0f;

    /**
     * The maximum possible value for the magnitude of this vector.
     */
    public float maxMagnitude = 0f;

    @Override
    public final String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(BoundedVector.class.getName());
        buffer.append(" [magnitude=");
        buffer.append(magnitude);
        buffer.append(", directionCompassDegrees=");
        buffer.append(directionCompassDegrees);
        buffer.append(", directionCompassRadians=");
        buffer.append(directionCompassRadians);
        buffer.append(", directionJavaDegrees=");
        buffer.append(directionJavaDegrees);
        buffer.append(", directionJavaRadians=");
        buffer.append(directionJavaRadians);
        buffer.append(", easterlyComponent=");
        buffer.append(easterlyComponent);
        buffer.append(", southerlyComponent=");
        buffer.append(southerlyComponent);
        buffer.append("]");
        return buffer.toString();
    }
}