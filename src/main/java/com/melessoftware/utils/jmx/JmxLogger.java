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
import javax.management.ObjectInstance;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
            return;
        }
        final Client client = maybeClient;

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

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        try {
            Set<ObjectInstance> objects = client.objects(objectName);
            for (ObjectInstance object : objects) {
                System.out.println(client.readAttribute(object.getObjectName(), attributeName));
            }
        } catch (MalformedObjectNameException mone) {
            throw new RuntimeException("TODO");
        } catch (Exception e) {
            throw new RuntimeException("TODO");
        }

        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Set<ObjectInstance> objects = null;
                try {
                    objects = client.objects(objectName);
                    for (ObjectInstance object : objects) {
                        System.out.println(client.readAttribute(object.getObjectName(), attributeName));
                    }
                } catch (MalformedObjectNameException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (Exception e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);

    }
}
