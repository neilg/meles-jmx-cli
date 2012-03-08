/*
 * Copyright (c) 2012 Neil Green
 *
 * This file is part of Meles Utils.
 *
 * Meles Utils is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Meles Utils is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Meles Utils.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.melessoftware.utils.jmx;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.IOException;
import java.util.Set;

public class ListObjectsCallback implements MBeanServerCallback<Void> {

    private ObjectName objectNamePattern;
    private String attributeName;
    private Appendable out;

    public ListObjectsCallback(ObjectName objectNamePattern, String attributeName, Appendable out) {
        this.objectNamePattern = objectNamePattern;
        this.attributeName = attributeName;
        this.out = out;
    }
    
    public ListObjectsCallback(String objectNamePattern, String attributeName, Appendable out) throws MalformedObjectNameException {
        this(new ObjectName(objectNamePattern), attributeName, out);
    }

    @Override
    public Void execute(MBeanServerConnection connection) throws IOException {
        Set<ObjectInstance> objects = connection.queryMBeans(objectNamePattern, null);
        for(ObjectInstance object : objects) {
            ObjectName objectName = object.getObjectName();

            out.append(objectName.toString());
            if(attributeName!=null) {
                try {
                    Object attributeValue = connection.getAttribute(objectName, attributeName);
                    out.append("\t ");
                    out.append(attributeValue.toString());
                } catch (MBeanException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (AttributeNotFoundException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (InstanceNotFoundException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (ReflectionException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            out.append(System.getProperty("line.separator"));
        }

        return null;
    }
}
