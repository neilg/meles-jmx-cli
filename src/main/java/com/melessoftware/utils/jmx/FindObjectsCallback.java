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

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Set;

public class FindObjectsCallback implements MBeanServerCallback<Set<ObjectInstance>> {

    private ObjectName objectNamePattern;

    public FindObjectsCallback(ObjectName objectNamePattern) {
        this.objectNamePattern = objectNamePattern;
    }

    public FindObjectsCallback(String objectNamePattern) throws MalformedObjectNameException {
        this(new ObjectName(objectNamePattern));
    }

    @Override
    public Set<ObjectInstance> execute(MBeanServerConnection connection) throws IOException {
        return connection.queryMBeans(objectNamePattern, null);
    }
}
