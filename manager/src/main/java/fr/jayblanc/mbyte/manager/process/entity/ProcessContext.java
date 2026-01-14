/*
 * Copyright (C) 2025 Jerome Blanchard <jayblanc@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.jayblanc.mbyte.manager.process.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jerome Blanchard
 */
public class ProcessContext {

    private static final Logger LOGGER = Logger.getLogger(ProcessContext.class.getName());

    public static final String GLOBAL_SCOPE = "global";
    public static final String TASK_SCOPE_PREFIX = "task_";

    private StringBuilder logger;
    private Map<String, Map<String, Serializable>> entries;

    public ProcessContext() {
        this.logger = new StringBuilder();
        this.entries = new HashMap<>();
    }

    public StringBuilder getLogger() {
        return logger;
    }

    public void setLogger(StringBuilder logger) {
        this.logger = logger;
    }

    public Map<String, Map<String, Serializable>> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, Map<String, Serializable>> entries) {
        this.entries = entries;
    }

    public ProcessContext appendLog(String log) {
        this.logger.append(log);
        return this;
    }

    public ProcessContext appendLog(Object log) {
        this.logger.append(log);
        return this;
    }

    @JsonIgnore
    public String getLog() {
        return logger.toString();
    }

    @JsonIgnore
    public String getStringValue(String key) {
        return getStringValue(GLOBAL_SCOPE, key);
    }

    @JsonIgnore
    public String getStringValue(String scope, String key) {
        Map<String, Serializable> scopeEntries = getScopeEntries(scope);
        if (!scopeEntries.containsKey(key)) {
            scopeEntries = getScopeEntries(GLOBAL_SCOPE);
        }
        Object value = scopeEntries.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    @JsonIgnore
    public <T> T getValue(String key, Class<T> clazz) {
        return getValue(GLOBAL_SCOPE, key, clazz);
    }

    @JsonIgnore
    public <T> T getValue(String scope, String key, Class<T> clazz) {
        Map<String, Serializable> scopeEntries = getScopeEntries(scope);
        if (!scopeEntries.containsKey(key)) {
            scopeEntries = getScopeEntries(GLOBAL_SCOPE);
        }
        Object value = scopeEntries.get(key);
        if (value == null || clazz == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
    }

    public void setValue(String key, Serializable value) {
        this.setValue(GLOBAL_SCOPE, key, value);
    }

    public void setValue(String scope, String key, Serializable value) {
        LOGGER.log(Level.INFO, "Setting process context value: scope=" + scope + ", key=" + key + ", value=" + value);
        this.getScopeEntries(scope).put(key, value);
    }

    public void unsetValue(String key) {
        this.unsetValue(GLOBAL_SCOPE, key);
    }

    public void unsetValue(String scope, String key) {
        LOGGER.log(Level.INFO, "Unsetting process context value: scope=" + scope + ", key=" + key);
        this.getScopeEntries(scope).remove(key);
    }

    private Map<String, Serializable> getScopeEntries(String scope) {
        return this.entries.computeIfAbsent(scope, k -> new HashMap<>());
    }

}
