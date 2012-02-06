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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class LogAttributeCallback implements MBeanServerCallback<Void> {

    private static final String LOG_PATTERN = "{} {} {}";

    private ObjectName objectNamePattern;
    private String attributeName;
    private Logger logger;

    public LogAttributeCallback(String objectNamePattern, String attributeName, String logger) throws MalformedObjectNameException {
        this(new ObjectName(objectNamePattern), attributeName, LoggerFactory.getLogger(logger));
    }

    public LogAttributeCallback(ObjectName objectNamePattern, String attributeName, Logger logger) {
        this.objectNamePattern = objectNamePattern;
        this.attributeName = attributeName;
        this.logger = logger;
    }

    @Override
    public Void execute(MBeanServerConnection connection) throws IOException {
        Set<ObjectInstance> objects = connection.queryMBeans(objectNamePattern, null);
        for (ObjectInstance object : objects) {
            ObjectName objectName = object.getObjectName();
            try {
                Object attributeValue = connection.getAttribute(objectName, attributeName);
                logger.info(LOG_PATTERN, new Object[]{objectName, attributeName, attributeValue});
            } catch (MBeanException e) {
                // wraps exception thrown by mbean's getter
                logger.error("{} MBean threw Exception executing getter for {} {}", new Object[]{objectName, attributeName, e.getTargetException()});
            } catch (AttributeNotFoundException e) {
                // attribute not accessible in mbean
                logger.error("attribute {} was not accessible in {} MBean", attributeName, objectName);
            } catch (InstanceNotFoundException e) {
                // mbean doesn't exist on server
                // There is a small race window for this to happen, but it's quite unlikely. The server has just told us that the object exists.
                logger.debug("MBean {} disappeared", objectName);
            } catch (ReflectionException e) {
                // wraps Exception thrown when trying to invoke getter (javadoc for connection.getAttribute says "invoke setter"???)
                logger.error("{} MBean threw Exception executing getter for {} {}", new Object[]{objectName, attributeName, e.getTargetException()});
            }
        }

        return null;
    }
}
