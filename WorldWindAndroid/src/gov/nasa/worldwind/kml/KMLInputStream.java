/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.kml.io.KMLDoc;
import gov.nasa.worldwind.util.*;

import java.io.*;
import java.net.*;

/**
 * Implements the {@link gov.nasa.worldwind.kml.io.KMLDoc} interface for KML files read directly from input streams.
 *
 * @author tag
 * @version $Id: KMLInputStream.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLInputStream implements KMLDoc
{
    /** The {@link java.io.InputStream} specified to the constructor. */
    protected InputStream inputStream;

    /** The URI of this KML document. May be {@code null}. */
    protected URI uri;

    /**
     * Construct a <code>KMLInputStream</code> instance.
     *
     * @param sourceStream the KML stream.
     * @param uri          the URI of this KML document. This URI is used to resolve relative references. May be {@code
     *                     null}.
     *
     * @throws IllegalArgumentException if the specified input stream is null.
     * @throws java.io.IOException              if an error occurs while attempting to read from the stream.
     */
    public KMLInputStream(InputStream sourceStream, URI uri) throws IOException
    {
        if (sourceStream == null)
        {
            String message = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        this.inputStream = sourceStream;
        this.uri = uri;
    }

    /**
     * Returns the input stream reference passed to the constructor.
     *
     * @return the input stream reference passed to the constructor.
     */
    public InputStream getKMLStream() throws IOException
    {
        return this.inputStream;
    }

    /**
     * Resolve references against the document's URI.
     *
     * @param path the path of the requested file.
     *
     * @return an input stream positioned to the start of the file, or null if the file does not exist.
     *
     * @throws IOException if an error occurs while attempting to query or open the file.
     */
    public InputStream getSupportFileStream(String path) throws IOException
    {
        if (path == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        String ref = this.getSupportFilePath(path);
        if (ref != null)
        {
            URL url = WWIO.makeURL(path);
            if (url != null)
                return url.openStream();
        }
        return null;
    }

    /**
     * Resolve references against the document's URI.
     *
     * @param path the path of the requested file.
     *
     * @return a URL to the requested file, or {@code null} if the document does not have a base URI.
     */
    public String getSupportFilePath(String path)
    {
        if (path == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        if (this.uri != null)
        {
            URI remoteFile = uri.resolve(path);
            if (remoteFile != null)
                return remoteFile.toString();
        }
        return null;
    }
}
