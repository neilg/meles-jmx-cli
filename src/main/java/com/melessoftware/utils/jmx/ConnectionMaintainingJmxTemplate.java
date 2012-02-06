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

public class ConnectionMaintainingJmxTemplate implements JmxTemplate {

    private static final Logger log = LoggerFactory.getLogger(ConnectionMaintainingJmxTemplate.class);

    private JMXServiceURL jmxUrl;

    private JMXConnector connector;
    private MBeanServerConnection connection;

    public ConnectionMaintainingJmxTemplate(JMXServiceURL jmxUrl) {
        this.jmxUrl = jmxUrl;
    }

    @Override
    public <T> T runWithConnection(MBeanServerCallback<T> callback) throws IOException {
        MBeanServerConnection currentConnection = getConnection();
        T result = null;
        try {
            result = callback.execute(currentConnection);
        } catch (RuntimeException re) {
            handleError(re);
        } catch (Error e) {
            handleError(e);
        }
        return result;
    }

    public void close() throws IOException {
        cleanup();
    }

    private <E extends Throwable> void handleError(E error) throws E {
        try {
            cleanup();
        } catch (IOException ioe) {
            // TODO what should we do with this?
            // we should either 1) log the ioe and throw the parameter error, or 2) log the error and throw the ioe
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
