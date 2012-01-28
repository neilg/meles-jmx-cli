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

import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JmxLogger {

    private static final int EXIT_STATUS_INVALID_ARGS = -1;

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        OptionSpec<String> urlSpec = parser.accepts("u", "JMX service url").withRequiredArg().required();
        OptionSpec<String> objectNameSpec = parser.accepts("o", "object name").withRequiredArg().required();
        OptionSpec<String> attributeNameSpec = parser.accepts("a", "attribute name").withRequiredArg().required();

        OptionSet options = null;
        try {
            options = parser.parse(args);
        } catch (OptionException oe) {
            System.err.println(oe.getMessage());
            parser.printHelpOn(System.err);
            System.exit(EXIT_STATUS_INVALID_ARGS);
        }

        String url = options.valueOf(urlSpec);
        final String objectName = options.valueOf(objectNameSpec);
        final String attributeName = options.valueOf(attributeNameSpec);

        final Client client = createClient(url);

        closeClientOnShutdown(client);

        final LogAttributeCommand command;
        try {
            command = new LogAttributeCommand(objectName, attributeName, "JmxLogger");
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("TODO", e);
        }

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    client.execute(command);
                } catch (IOException e) {
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);

    }

    private static void closeClientOnShutdown(final Client client) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (client != null) {
                    try {
                        client.disconnect();
                    } catch (IOException ioe) {
                        // we're shutting down anyway. Don't worry about it
                    }
                }
            }
        });
    }

    private static Client createClient(String url) {
        Client maybeClient;
        try {
            maybeClient = new Client(url);
        } catch (MalformedURLException mue) {
            String message = mue.getMessage();
            if (message == null) {
                System.err.printf("Invalid JMX URL: %s%n", url);
            } else {
                System.err.printf("Invalid JMX URL: %s, %s%n", url, message);
            }
            System.exit(EXIT_STATUS_INVALID_ARGS);
            return null;
        }
        return maybeClient;
    }
}
