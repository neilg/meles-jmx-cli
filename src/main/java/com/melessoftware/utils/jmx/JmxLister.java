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

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;

public class JmxLister {

    private static final int EXIT_STATUS_INVALID_ARGS = -1;

    private static final int EXIT_STATUS_COMMUNICATION = 1;

    public static void main(String[] args) throws IOException {

        OptionParser parser = new OptionParser();
        OptionSpec<String> urlSpec = parser.accepts("u", "JMX service url").withRequiredArg().required();
        OptionSpec<String> objectNameSpec = parser.accepts("n", "ObjectName pattern").withRequiredArg().defaultsTo("*:*");

        OptionSet options = null;
        try {
            options = parser.parse(args);
        } catch (OptionException oe) {
            System.err.println(oe.getMessage());
            parser.printHelpOn(System.err);
            System.exit(EXIT_STATUS_INVALID_ARGS);
        }

        String url = options.valueOf(urlSpec);
        String objectNamePattern = options.valueOf(objectNameSpec);
        JmxLister lister = null;
        try {
            lister = new JmxLister(url);
        } catch (MalformedURLException mue) {
            String message = mue.getMessage();
            if (message == null) {
                System.err.printf("Invalid JMX URL: %s%n", url);
            } else {
                System.err.printf("Invalid JMX URL: %s, %s%n", url, message);
            }
            System.exit(EXIT_STATUS_INVALID_ARGS);
        }
        try {
            lister.list(objectNamePattern, System.out);
        } catch (MalformedURLException mue) {
            String message = mue.getMessage();
            if (message == null) {
                System.err.printf("Invalid JMX URL: %s%n", url);
            } else {
                System.err.printf("Invalid JMX URL: %s, %s%n", url, message);
            }
            System.exit(EXIT_STATUS_INVALID_ARGS);
        } catch (IOException e) {
            String message = e.getMessage();
            if (message == null) {
                System.err.printf("Failure communicating with %s%n", url);
            } else {
                System.err.printf("Failure communicating with %s: %s%n", url, message);
            }
            System.exit(EXIT_STATUS_COMMUNICATION);
        } catch (MalformedObjectNameException mone) {
            String message = mone.getMessage();
            if(message==null) {
                System.err.printf("Invalid ObjectName pattern: %s%n", objectNamePattern);
            } else {
                System.err.printf("Invalid ObjectName pattern: %s, %s%n", objectNamePattern, message);
            }
            System.exit(EXIT_STATUS_INVALID_ARGS);
        }
    }

    private JMXServiceURL url;

    public JmxLister(String url) throws MalformedURLException {
        this(new JMXServiceURL(url));
    }

    public JmxLister(JMXServiceURL url) {
        this.url = url;
    }

    public void list(String objectNamePattern, Appendable out) throws IOException, MalformedObjectNameException {
        JMXConnector connector = JMXConnectorFactory.connect(url);
        try {
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            for (ObjectInstance objectInstance : connection.queryMBeans(new ObjectName(objectNamePattern), null)) {
                System.out.println(objectInstance.getObjectName());
            }
        } finally {
            connector.close();
        }
    }

}
