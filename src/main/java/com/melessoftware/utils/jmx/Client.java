/*
 * Copyright (c) 2011 Neil Green
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

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;

public class Client {

    private JMXServiceURL serviceUrl;

    private JMXConnector connector;

    public Client(String serviceUrl) throws MalformedURLException {
        this(new JMXServiceURL(serviceUrl));
    }

    public Client(JMXServiceURL serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public void disconnect() throws IOException {
        if (connector != null) {
            try {
                connector.close();
            } finally {
                connector = null;
            }
        }
    }

    public String[] domains() throws IOException {
        try {
            return connection().getDomains();
        } catch (IOException ioe) {
            disconnect();
            throw ioe;
        }
    }

    public Set<ObjectInstance> objects(String objectNamePattern) throws MalformedObjectNameException, IOException {
        try {
            return connection().queryMBeans(new ObjectName(objectNamePattern), null);
        } catch (IOException ioe) {
            disconnect();
            throw ioe;
        }
    }

    public Set<ObjectInstance> objects(String objectNamePattern, String attributeName, String attributeValue) throws MalformedObjectNameException, IOException {
        try {
            return connection().queryMBeans(new ObjectName(objectNamePattern), Query.eq(Query.attr(attributeName), Query.value(attributeValue)));
        } catch (IOException ioe) {
            disconnect();
            throw ioe;
        }
    }

    public Object readAttribute(ObjectName objectName, String attributeName) throws IOException, InstanceNotFoundException, ReflectionException, AttributeNotFoundException, MBeanException {
        return connection().getAttribute(objectName, attributeName);
    }

    public void writeAttribute(ObjectName objectName, String attributeName, Object attributeValue) throws IOException, InstanceNotFoundException, InvalidAttributeValueException, ReflectionException, AttributeNotFoundException, MBeanException {
        Attribute attribute = new Attribute(attributeName, attributeValue);
        connection().setAttribute(objectName, attribute);
    }

    private MBeanServerConnection connection() throws IOException {
        if (connector == null) {
            connector = JMXConnectorFactory.connect(serviceUrl);
        }
        return connector.getMBeanServerConnection();
    }
}
