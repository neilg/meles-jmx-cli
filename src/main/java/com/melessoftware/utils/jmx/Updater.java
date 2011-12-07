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

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Updater {

    private static final int EXIT_STATUS_INVALID_ARGS = -1;

    public static void main(String[] args) throws IOException, MalformedObjectNameException, InstanceNotFoundException, AttributeNotFoundException, ReflectionException, MBeanException, InvalidAttributeValueException {
        OptionParser parser = new OptionParser();
        OptionSpec<String> urlSpec = parser.accepts("u", "JMX service url").withRequiredArg().required();
        OptionSpec<String> objectNameSpec = parser.accepts("o", "ObjectName (pattern)").withRequiredArg().required();
        OptionSpec<String> attributeFilterSpec = parser.accepts("f", "Attribute filter").withRequiredArg();

        OptionSet options = null;
        try {
            options = parser.parse(args);
        } catch (OptionException oe) {
            System.err.println(oe.getMessage());
            parser.printHelpOn(System.err);
            System.exit(EXIT_STATUS_INVALID_ARGS);
        }

        List<String> urls = options.valuesOf(urlSpec);
        String objectNamePattern = options.valueOf(objectNameSpec);
        String attributeFilter = options.valueOf(attributeFilterSpec);

        List<String> update = options.nonOptionArguments();
        if (update.size() != 2) {
            System.err.println("there should be excatly one update attribute name and one update attribute value");
            System.exit(EXIT_STATUS_INVALID_ARGS);
        }

        String updateAttributeName = update.get(0);
        String updateAttributeValue = update.get(1);

        String filterAttributeName = null;
        String filterAttributeValue = null;
        if (attributeFilter != null) {
            String[] attributeFilterParts = attributeFilter.split("=");
            if (attributeFilterParts.length < 2) {
                System.err.println("there is no '=' in attribute filter, aborting");
                System.exit(EXIT_STATUS_INVALID_ARGS);
            }
            if (attributeFilterParts.length > 2) {
                System.err.println("there is more than one '=' in attribute filter, aborting");
                System.exit(EXIT_STATUS_INVALID_ARGS);
            }
            filterAttributeName = attributeFilterParts[0];
            filterAttributeValue = attributeFilterParts[1];
        }

        Map<String, Client> clients = new HashMap<String, Client>();
        for (String url : urls) {
            final Client client = new Client(url);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        client.disconnect();
                    } catch (IOException ioe) {
                        // ignore it, we're shutting down
                    }
                }
            });
            clients.put(url, client);
        }

        class UpdateInfo {
            ObjectName objectName;
            String filterAttributeName;
            Object discoveredFilterAttributeValue;
            Object currentUpdateAttributeValue;

            public String updateObjectId() {
                if (filterAttributeName == null) {
                    return String.format("%s", objectName);
                } else {
                    return String.format("%s (%s=%s)", objectName, filterAttributeName, discoveredFilterAttributeValue);
                }
            }
        }

        Map<String, List<UpdateInfo>> proposedUpdatesByUrl = new HashMap<String, List<UpdateInfo>>();

        for (String url : urls) {
            Client client = clients.get(url);
            proposedUpdatesByUrl.put(url, new ArrayList<UpdateInfo>());
            Set<ObjectInstance> objectInstances = (filterAttributeName == null) ? client.objects(objectNamePattern) : client.objects(objectNamePattern, filterAttributeName, filterAttributeValue);
            for (ObjectInstance objectInstance : objectInstances) {
                UpdateInfo updateInfo = new UpdateInfo();
                updateInfo.objectName = objectInstance.getObjectName();
                updateInfo.currentUpdateAttributeValue = client.readAttribute(updateInfo.objectName, updateAttributeName);
                if (filterAttributeName != null) {
                    updateInfo.filterAttributeName = filterAttributeName;
                    updateInfo.discoveredFilterAttributeValue = client.readAttribute(updateInfo.objectName, filterAttributeName);
                }
                proposedUpdatesByUrl.get(url).add(updateInfo);
            }
        }

        for (String url : proposedUpdatesByUrl.keySet()) {
            System.out.println();
            System.out.println(url);
            for (UpdateInfo updateInfo : proposedUpdatesByUrl.get(url)) {
                System.out.printf("\t%s\t %s%n", updateInfo.updateObjectId(), updateInfo.currentUpdateAttributeValue);
            }
        }

        System.out.println();
        System.out.printf("Update %s (with current values as above) to %s  ? (y/N)%n", updateAttributeName, updateAttributeValue);
        System.out.flush();
        String response = new BufferedReader(new InputStreamReader(System.in)).readLine();

        if (!response.equalsIgnoreCase("y")) {
            System.out.println("Terminating at user request");
            System.exit(0);
        }

        for (String url : proposedUpdatesByUrl.keySet()) {
            Client client = clients.get(url);
            System.out.println(url);
            for (UpdateInfo updateInfo : proposedUpdatesByUrl.get(url)) {
                client.writeAttribute(updateInfo.objectName, updateAttributeName, updateAttributeValue);
                Object updatedAttributeValue = client.readAttribute(updateInfo.objectName, updateAttributeName);
                if(updateAttributeValue.equals(updatedAttributeValue)) {
                    // it's as expected
                    System.out.printf("\t%s\t %s%n", updateInfo.updateObjectId(), updatedAttributeValue);
                } else {
                    // hasn't updated
                    System.out.printf("\tNOT UPDATED %s\t %s%n", updateInfo.updateObjectId(), updatedAttributeValue);
                }
                System.out.flush();
            }
            System.out.println();
        }

    }

}
