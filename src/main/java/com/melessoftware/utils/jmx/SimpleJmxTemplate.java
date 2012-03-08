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
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;

public class SimpleJmxTemplate implements JmxTemplate {

    private JMXServiceURL jmxUrl;

    public SimpleJmxTemplate(String jmxUrl) throws MalformedURLException {
        this(new JMXServiceURL(jmxUrl));
    }

    public SimpleJmxTemplate(JMXServiceURL jmxUrl) {
        this.jmxUrl = jmxUrl;
    }

    @Override
    public <T> T runWithConnection(MBeanServerCallback<T> callback) throws IOException {
        JMXConnector connector = JMXConnectorFactory.connect(jmxUrl);
        try {
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            return callback.execute(connection);
        } finally {
            connector.close();
        }
    }

    @Override
    public void close() {
        // don't need to do anything as we don't maintain resources
    }
}
