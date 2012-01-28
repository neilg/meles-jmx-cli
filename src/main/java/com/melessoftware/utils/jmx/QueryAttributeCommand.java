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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class QueryAttributeCommand implements Command<List<QueryAttributeResult>> {

    private ObjectName objectNamePattern;
    private String attributeName;

    public QueryAttributeCommand(ObjectName objectNamePattern, String attributeName) {
        this.objectNamePattern = objectNamePattern;
        this.attributeName = attributeName;
    }

    public QueryAttributeCommand(String objectNamePattern, String attributeName) throws MalformedObjectNameException {
        this(new ObjectName(objectNamePattern), attributeName);
    }

    @Override
    public List<QueryAttributeResult> execute(MBeanServerConnection connection) throws IOException {
        Set<ObjectInstance> objects = connection.queryMBeans(objectNamePattern, null);
        List<QueryAttributeResult> results = new ArrayList<QueryAttributeResult>(objects.size());
        for (ObjectInstance object : objects) {
            ObjectName objectName = object.getObjectName();
            try {
                Object attributeValue = connection.getAttribute(objectName, attributeName);
                results.add(new AttributeValueResult(objectName, attributeName, attributeValue));
            } catch (MBeanException e) {
                // wraps exception thrown by mbean's getter
                results.add(new FailedQueryResult(objectName, attributeName, e));
            } catch (AttributeNotFoundException e) {
                // attribute not accessible in mbean
                results.add(new FailedQueryResult(objectName, attributeName, e));
            } catch (InstanceNotFoundException e) {
                // mbean doesn't exist on server
                // There is a small race window for this to happen, but it's quite unlikely. The server has just told us that the object exists.
                results.add(new FailedQueryResult(objectName, attributeName, e));
            } catch (ReflectionException e) {
                // wraps Exception thrown when trying to invoke getter (javadoc for connection.getAttribute says "invoke setter"???)
                results.add(new FailedQueryResult(objectName, attributeName, e));
            }
        }
        return results;
    }
}
