/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.util;

import java.io.File;
import java.io.IOException;

/**
 * A class that attempts to embody OpenOffice's rules for encoding file URIs which appear to differ from Java's. A
 * Windows style path is always prefixed "file:///" whereas a unix one is prefixed "file://".
 * 
 * @author dward
 */
public class OpenOfficeURI
{

    /** The source file. */
    private File source;

    /**
     * Instantiates a new open office URI.
     * 
     * @param source
     *            the source file name to convert to a URI
     * @throws IOException
     *             if the string cannot be resolved to a canonical file path
     */
    public OpenOfficeURI(String source) throws IOException
    {
        this.source = new File(source.replaceAll(" ", "%20")).getCanonicalFile();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        String absolute = this.source.getAbsolutePath();
        if (File.separatorChar != '/')
        {
            absolute = absolute.replace(File.separatorChar, '/');
        }
        return (absolute.startsWith("/") ? "file://" : "file:///") + absolute;
    }

}
