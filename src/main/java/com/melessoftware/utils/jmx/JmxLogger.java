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
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JmxLogger {

    private static final int EXIT_STATUS_INVALID_ARGS = -1;

    private static final Logger LOG = LoggerFactory.getLogger(JmxLogger.class);

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        OptionSpec<String> urlSpec = parser.accepts("u", "JMX service url").withRequiredArg().required();
        OptionSpec<String> objectNameSpec = parser.accepts("o", "find objects mathcing this pattern").withRequiredArg().required();
        OptionSpec<String> attributeNameSpec = parser.accepts("a", "show the value of this attribute").withRequiredArg().required();

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

        PersistentJmxTemplate template = new PersistentJmxTemplate(url);
        closeTemplateOnShutdown(template);

        LogAttributeCallback callback;
        try {
            callback = new LogAttributeCallback(objectName, attributeName, "JmxLogger");
        } catch (MalformedObjectNameException mone) {
            String message = mone.getMessage();
            if (message == null) {
                System.err.printf("Invalid object name: %s%n", objectName);
            } else {
                System.err.printf("Invalid object name: %s, %s%n", objectName, message);
            }
            System.exit(EXIT_STATUS_INVALID_ARGS);
            return;
        }

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(logAttributeTask(template, callback), 0, 1000, TimeUnit.MILLISECONDS);
    }

    private static Runnable logAttributeTask(final JmxTemplate template, final MBeanServerCallback<?> callback) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    template.runWithConnection(callback);
                } catch (IOException ioe) {
                    LOG.debug("exception executing query", ioe);
                }
            }
        };
    }

    private static void closeTemplateOnShutdown(final PersistentJmxTemplate template) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (template != null) {
                    try {
                        template.close();
                    } catch (IOException ioe) {
                        // we're shutting down anyway. Don't worry about it
                        LOG.trace("exception disconnecting client", ioe);
                    }
                }
            }
        });
    }

}
