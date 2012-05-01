package com.senseidb.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;

import com.linkedin.bobo.facets.FacetHandler;
import com.linkedin.bobo.facets.RuntimeFacetHandlerFactory;

public class SenseiPluginRegistry {
    public static final String FACET_CONF_PREFIX = "sensei.custom.facets";
    private Map<String, PluginHolder> pluginsByPrefix = new LinkedHashMap<String, PluginHolder>();
    private Map<String, PluginHolder> pluginsByNames = new LinkedHashMap<String, PluginHolder>();
    private List<PluginHolder> plugins = new ArrayList<PluginHolder>();
    private Configuration configuration;
    private static Map<Configuration, SenseiPluginRegistry> cachedRegistries = new IdentityHashMap<Configuration, SenseiPluginRegistry>();
    private SenseiPluginRegistry() {
    }

    public static synchronized SenseiPluginRegistry get(Configuration conf) {
        return cachedRegistries.get(conf);
    }

    public static String getNameByPrefix(String prefix) {
        if (prefix != null) {
            if (prefix.contains("."))
                return prefix.substring(prefix.lastIndexOf(".") + 1);
            else
                return prefix;
        }
        return null;
    }

    /**
     * Factory method.
     * It returns a new SenseiPluginRegistry when given a new conf, and will cache that instance
     * to return it in any subsequent call with the same conf.
     */
    public static synchronized SenseiPluginRegistry build(Configuration conf) {
        if (cachedRegistries.containsKey(conf)) {
            return cachedRegistries.get(conf);
        }

        SenseiPluginRegistry ret = new SenseiPluginRegistry();
        ret.configuration = conf;
        Iterator keysIterator = conf.getKeys();
        while (keysIterator.hasNext()) {
            String key = (String) keysIterator.next();
            if (key.endsWith(".class")) {
                String prefix = key.substring(0, key.indexOf(".class"));
                String pluginName = getNameByPrefix(prefix);
                String pluginCLass = conf.getString(key);
                PluginHolder holder = new PluginHolder(ret, pluginCLass, pluginName, prefix);
                ret.plugins.add(holder);
                ret.pluginsByPrefix.put(prefix, holder);
                ret.pluginsByNames.put(pluginName, holder);

                Iterator propertyIterator = conf.getKeys(prefix);
                while (propertyIterator.hasNext()) {
                    String propertyName = (String) propertyIterator.next();
                    if (propertyName.endsWith(".class")) {
                        continue;
                    }
                    String property = propertyName;
                    if (propertyName.contains(prefix)) {
                        property = propertyName.substring(prefix.length() + 1);
                    }
                    holder.properties.put(property, conf.getProperty(propertyName).toString());

                }
            }
        }
        cachedRegistries.put(conf, ret);
        return ret;
    }

    public <T> T getBeanByName(String name, Class<T> type) {
        PluginHolder holder = pluginsByNames.get(name);
        if (holder == null) {
            return null;
        }
        return (T) holder.getInstance();
    }

    public <T> List<T> getBeansByType(Class<T> type) {
        List<T> ret = new ArrayList<T>();
        for (PluginHolder pluginHolder : plugins) {
            if (pluginHolder.getInstance() != null && type.isAssignableFrom(pluginHolder.getInstance().getClass())) {
                ret.add((T) pluginHolder.getInstance());
            }
        }
        return ret;
    }

    public FacetHandler<?> getFacet(String name) {
        for (Object handlerObject : resolveBeansByListKey(FACET_CONF_PREFIX, Object.class)) {
            if (!(handlerObject instanceof FacetHandler)) {
                continue;
            }
            FacetHandler handler = (FacetHandler) handlerObject;
            if (handler.getName().equals(name)) {
                return handler;
            }
        }
        return null;
    }
    public RuntimeFacetHandlerFactory getRuntimeFacet(String name) {
        for (Object handlerObject : resolveBeansByListKey(FACET_CONF_PREFIX, Object.class)) {
            if (!(handlerObject instanceof RuntimeFacetHandlerFactory)) {
                continue;
            }
            RuntimeFacetHandlerFactory handler = (RuntimeFacetHandlerFactory) handlerObject;
            if (handler.getName().equals(name)) {
                return handler;
            }
        }
        return null;
    }
    public <T> T getBeanByFullPrefix(String fullPrefix, Class<T> type) {
        PluginHolder holder = pluginsByPrefix.get(fullPrefix);
        if (holder == null) {
            return null;
        }
        return (T) holder.getInstance();
    }

    public <T> List<T> resolveBeansByListKey(String paramKey, Class<T> returnedClass) {
        if (!paramKey.endsWith(".list")) {
            paramKey = paramKey + ".list";
        }

        List<T> ret = new ArrayList<T>();
        String strList = configuration.getString(paramKey);
        if (strList == null) {
            return null;
        }
        String[] keys = strList.split(",");
        if (keys == null || keys.length == 0) {
            return null;
        }
        for (String key : keys) {
            if (key.trim().length() == 0) {
                continue;
            }
            Object bean = getBeanByName(key.trim(), Object.class);
            if (bean == null) {
                bean = getBeanByFullPrefix(key.trim(), Object.class);
            }
            if (bean == null) {
                throw new IllegalStateException("the bean with name " + key + " couldn't be found");
            }
            if (bean instanceof Collection) {
                ret.addAll((Collection) bean);
            } else {
                ret.add((T) bean);
            }
        }
        return ret;
    }

    /**
     * Calls start on all the contained plugins.
     * For all contained plugins (non factories), it calls start(). It will skip plugin factories.
     */
    public synchronized void start() {
        for (PluginHolder pluginHolder : plugins) {
            Object instance = pluginHolder.getInstance();
            if (instance instanceof SenseiPlugin) {
                ((SenseiPlugin) instance).start();
            }
        }
    }

    /**
     * Calls stop on all the contained plugins.
     * Fall all contained plugins (non factories), it calls stop() on them. WARNING: plugin instances
     * created from plugin factories have to be stopped separately.
     */
    public synchronized void stop() {
        for (PluginHolder pluginHolder : plugins) {
            Object instance = pluginHolder.getInstance();
            if (instance instanceof SenseiPlugin) {
                ((SenseiPlugin) instance).stop();
            }
        }
        pluginsByPrefix.clear();
        pluginsByNames.clear();
        plugins.clear();
        cachedRegistries.remove(configuration);
        configuration = null;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

}
