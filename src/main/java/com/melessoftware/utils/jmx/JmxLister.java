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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;

public class JmxLister {

    private static final int EXIT_STATUS_INVALID_ARGS = -1;
    private static final int EXIT_STATUS_COMMUNICATION = 1;

    private static final Logger LOG = LoggerFactory.getLogger(JmxLister.class);

    public static void main(String[] args) throws IOException {

        OptionParser parser = new OptionParser();
        OptionSpec<String> urlSpec = parser.accepts("u", "JMX service url").withRequiredArg().required();
        OptionSpec<String> objectNameSpec = parser.accepts("n", "find objects with names matching this pattern").withRequiredArg().defaultsTo("*:*");
        OptionSpec<String> attributeNameSpec = parser.accepts("a", "show the value of this attribute").withRequiredArg();

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
        String attributeName = options.valueOf(attributeNameSpec);
        JmxLister lister = null;
        try {
            lister = new JmxLister(url);
        } catch (MalformedURLException mue) {
            exitMalformedUrl(url, mue);
        }
        try {
            lister.list(objectNamePattern, attributeName, System.out);
        } catch (MalformedURLException mue) {
            exitMalformedUrl(url, mue);
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
            if (message == null) {
                System.err.printf("Invalid ObjectName pattern: %s%n", objectNamePattern);
            } else {
                System.err.printf("Invalid ObjectName pattern: %s, %s%n", objectNamePattern, message);
            }
            System.exit(EXIT_STATUS_INVALID_ARGS);
        }
    }

    private static void exitMalformedUrl(String url, MalformedURLException mue) {
        String message = mue.getMessage();
        if (message == null) {
            System.err.printf("Invalid JMX URL: %s%n", url);
        } else {
            System.err.printf("Invalid JMX URL: %s, %s%n", url, message);
        }
        System.exit(EXIT_STATUS_INVALID_ARGS);
    }

    private JMXServiceURL url;

    public JmxLister(String url) throws MalformedURLException {
        this(new JMXServiceURL(url));
    }

    public JmxLister(JMXServiceURL url) {
        this.url = url;
    }

    public void list(String objectNamePattern, String attributeName, Appendable out) throws IOException, MalformedObjectNameException {
        JmxTemplate template = new SimpleJmxTemplate(url);
        try {
            template.runWithConnection(new ListObjectsCallback(objectNamePattern, attributeName, out, LOG));
        } finally {
            template.close();
        }
    }

}
