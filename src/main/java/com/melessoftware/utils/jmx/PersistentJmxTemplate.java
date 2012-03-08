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

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;

public class PersistentJmxTemplate implements JmxTemplate {

    private static final Logger log = LoggerFactory.getLogger(PersistentJmxTemplate.class);

    private JMXServiceURL jmxUrl;

    private JMXConnector connector;
    private MBeanServerConnection connection;

    public PersistentJmxTemplate(JMXServiceURL jmxUrl) {
        this.jmxUrl = jmxUrl;
    }

    public PersistentJmxTemplate(String jmxUrl) throws MalformedURLException {
        this(new JMXServiceURL(jmxUrl));
    }

    @Override
    public <T> T runWithConnection(MBeanServerCallback<T> callback) throws IOException {
        MBeanServerConnection currentConnection = getConnection();
        T result = null;
        try {
            result = callback.execute(currentConnection);
        } catch (IOException ioe) {
            handleError(ioe);
        } catch (RuntimeException re) {
            handleError(re);
        } catch (Error e) {
            handleError(e);
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        cleanup();
    }

    private <E extends Throwable> void handleError(E error) throws E {
        try {
            cleanup();
        } catch (IOException ioe) {
            log.error("IOException while handling {}. Stack trace logged at debug", error.getClass());
            log.debug("IOException while handling error", ioe);
        }
        throw error;
    }

    private MBeanServerConnection getConnection() throws IOException {
        if (connection == null) {
            if (connector == null) {
                connector = JMXConnectorFactory.connect(jmxUrl);
            }
            try {
                connection = connector.getMBeanServerConnection();
            } finally {
                if (connection == null) {
                    terminateConnector();
                }
            }
        }
        return connection;
    }

    private void cleanup() throws IOException {
        connection = null;
        terminateConnector();
    }

    private void terminateConnector() throws IOException {
        try {
            connector.close();
        } finally {
            connector = null;
        }
    }

}
