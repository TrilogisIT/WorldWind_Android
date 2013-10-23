/* Copyright (C) 2001, 2012 United States Government as represented by 
the Administrator of the National Aeronautics and Space Administration. 
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import java.nio.*;

/**
 * BufferUtil provides a collection of static utility methods for working with buffers that are used to submit vertex
 * attribute data and element data to OpenGL.
 * <p/>
 * <strong>Creating OpenGL Buffers</strong>
 * <p/>
 * Vertex attribute data and element data is submitted to OpenGL and OpenGL ES using Java's New I/O Buffers. Since these
 * buffers are used by both the Java virtual machine and the OpenGL driver, they must reference direct system memory and
 * have their elements stored in the platform byte order. BufferUtil provides methods for creating buffers that meet
 * these criteria: newByteBuffer, newShortBuffer, and newFloatBuffer.
 * <p/>
 * World Wind's use of buffers to store vertex attributes and elements is based on the capabilities of OpenGL ES 2.0.
 * The OpenGL ES 2.0 system accepts vertex attributes stored in bytes, shorts, and floats, and accepts elements stored
 * in bytes and shorts. Given these limitations, BufferUtil provides only the methods necessary for working with buffers
 * representing these data types.
 *
 * @author dcollins
 * @version $Id: BufferUtil.java 813 2012-09-26 22:05:34Z dcollins $
 */
public class BufferUtil
{
    /**
     * Allocates and returns a new byte buffer with the specified capacity, in number of byte elements. The returned
     * buffer is a direct byte buffer, and its byte order is set to the current platform byte order. See the section
     * above on <i>Creating Vertex Attribute Buffers</i> for more information.
     *
     * @param capacity the new buffer's capacity, in bytes.
     *
     * @return a new byte buffer with the specified capacity.
     *
     * @throws IllegalArgumentException if the capacity is less than 0.
     */
    public static ByteBuffer newByteBuffer(int capacity)
    {
        if (capacity < 0)
        {
            String msg = Logging.getMessage("generic.CapacityIsInvalid", capacity);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    /**
     * Allocates and returns a new short buffer with the specified capacity, in number of short elements. The returned
     * buffer is backed by a direct byte buffer who's byte order is set to the current platform byte order. See the
     * section above on <i>Creating Vertex Attribute Buffers</i> for more information.
     *
     * @param capacity the new buffer's capacity, in number of short elements.
     *
     * @return a new short buffer with the specified capacity.
     *
     * @throws IllegalArgumentException if the capacity is less than 0.
     */
    public static ShortBuffer newShortBuffer(int capacity)
    {
        if (capacity < 0)
        {
            String msg = Logging.getMessage("generic.CapacityIsInvalid", capacity);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        return ByteBuffer.allocateDirect(Short.SIZE / 8 * capacity).order(ByteOrder.nativeOrder()).asShortBuffer();
    }

    /**
     * Allocates and returns a new float buffer with the specified capacity, in number of float elements. The returned
     * buffer is backed by a direct byte buffer who's byte order is set to the current platform byte order. See the
     * section above on <i>Creating Vertex Attribute Buffers</i> for more information.
     *
     * @param capacity the new buffer's capacity, in number of float elements.
     *
     * @return a new float buffer with the specified capacity.
     *
     * @throws IllegalArgumentException if the capacity is less than 0.
     */
    public static FloatBuffer newFloatBuffer(int capacity)
    {
        if (capacity < 0)
        {
            String msg = Logging.getMessage("generic.CapacityIsInvalid", capacity);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        return ByteBuffer.allocateDirect(Float.SIZE / 8 * capacity).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }
}
