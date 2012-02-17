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

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class ProxyJmxTemplate implements JmxTemplate {

    private final JMXServiceURL jmxUrl;
    private ProxyMBeanServerConnection proxyMBeanServerConnection;
//    private MBeanServerConnection proxyMBeanServerConnection;

    public ProxyJmxTemplate(JMXServiceURL jmxUrl) {
        this.jmxUrl = jmxUrl;
        this.proxyMBeanServerConnection = new ProxyMBeanServerConnection();
//        ConnectionHandler handler = new ConnectionHandler();
//        Class<?> proxyClass = Proxy.getProxyClass(MBeanServerConnection.class.getClassLoader(), MBeanServerConnection.class);
//        try {
//            proxyMBeanServerConnection = (MBeanServerConnection) proxyClass.getConstructor(InvocationHandler.class).newInstance(handler);
//        } catch (InstantiationException e) {
//            throw new RuntimeException(e);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw new RuntimeException(e);
//        } catch (NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public <T> T runWithConnection(MBeanServerCallback<T> callback) throws IOException {
        return callback.execute(proxyMBeanServerConnection);
    }

    private class ConnectionHandler implements InvocationHandler {

        private JMXConnector connector;

        private MBeanServerConnection getConnection() throws IOException {
            if (connector == null) {
                connector = JMXConnectorFactory.connect(jmxUrl);
            }
            try {
                return connector.getMBeanServerConnection();
            } catch (IOException ioe) {
                closeConnector();
                throw ioe;
            }
        }

        private void closeConnector() throws IOException {
            JMXConnector connector = this.connector;
            this.connector = null;
            connector.close();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            MBeanServerConnection connection = getConnection();
            String methodName = method.getName();
            Method underlyingMethod = findUnderlyingMethod(connection, method.getName(), args);
            try {
                return underlyingMethod.invoke(connection, args);
            } catch (InvocationTargetException ite) {
                Throwable targetException = ite.getTargetException();
                if (targetException instanceof IOException) {
                    closeConnector();
                }
                throw targetException;
            }
        }

        private Method findUnderlyingMethod(MBeanServerConnection connection, String methodName, Object[] args) {
            Class<?>[] argTypes;
            if (args == null) {
                argTypes = null;
            } else {
                argTypes = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg == null) {
                        argTypes[i] = null;
                    } else {
                        argTypes[i] = arg.getClass();
                    }
                }
            }
            try {
                return connection.getClass().getMethod(methodName, argTypes);
            } catch (NoSuchMethodException nsme) {
                for (Method method : connection.getClass().getMethods()) {
                }
            }
            return null;  //To change body of created methods use File | Settings | File Templates.
        }
    }

    private class ProxyMBeanServerConnection implements MBeanServerConnection {

        private JMXConnector connector;

        private MBeanServerConnection getConnection() throws IOException {
            if (connector == null) {
                connector = JMXConnectorFactory.connect(jmxUrl);
            }
            try {
                return connector.getMBeanServerConnection();
            } catch (IOException ioe) {
                closeConnector();
                throw ioe;
            }
        }

        private void closeConnector() throws IOException {
            JMXConnector connector = this.connector;
            this.connector = null;
            connector.close();
        }

        private <T> T handleIOE(IOException ioe) throws IOException {
            closeConnector();
            throw ioe;
        }

        @Override
        public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
            try {
                return getConnection().createMBean(className, name);
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
            try {
                return getConnection().createMBean(className, name, loaderName);
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
            try {
                return getConnection().createMBean(className, name, params, signature);
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
            try {
                return getConnection().createMBean(className, name, loaderName, params, signature);
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException, IOException {
            try {
                getConnection().unregisterMBean(name);
            } catch (IOException ioe) {
                handleIOE(ioe);
            }
        }

        @Override
        public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException, IOException {
            try {
                return getConnection().getObjectInstance(name);
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException {
            try {
                return getConnection().queryMBeans(name, query);
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
            try {
                return getConnection().queryNames(name, query);
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public boolean isRegistered(ObjectName name) throws IOException {
            try {
                return getConnection().isRegistered(name);
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public Integer getMBeanCount() throws IOException {
            try {
                return getConnection().getMBeanCount();
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
            try {
                return getConnection().getAttribute(name, attribute);
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException, IOException {
            try {
                return getConnection().getAttributes(name, attributes);
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, IOException {
            try {
                getConnection().setAttribute(name, attribute);
            } catch (IOException ioe) {
                handleIOE(ioe);
            }
        }

        @Override
        public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException, IOException {
            try {
                return getConnection().setAttributes(name, attributes);
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
            try {
                return getConnection().invoke(name, operationName, params, signature);
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public String getDefaultDomain() throws IOException {
            try {
                return getConnection().getDefaultDomain();
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public String[] getDomains() throws IOException {
            try {
                return getConnection().getDomains();
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
            try {
                return getConnection().getMBeanInfo(name);
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }

        @Override
        public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException, IOException {
            try {
                return getConnection().isInstanceOf(name, className);
            } catch (IOException ioe) {
                return handleIOE(ioe);
            }
        }
    }

}
