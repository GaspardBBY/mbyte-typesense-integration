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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jerome Blanchard
 */
public class ProcessContext {

    private static final Logger LOGGER = Logger.getLogger(ProcessContext.class.getName());
    private static final String VAR_PREFIX = "$";

    public static final String GLOBAL_SCOPE = "global_scope";

    private Map<String, Map<String, Serializable>> entries;

    public ProcessContext() {
        this.entries = new HashMap<>();
    }

    public Map<String, Map<String, Serializable>> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, Map<String, Serializable>> entries) {
        this.entries = entries;
    }

    @JsonIgnore
    public Optional<String> getStringValue(String key) {
        return getStringValue(GLOBAL_SCOPE, key);
    }

    @JsonIgnore
    public Optional<String> getStringValue(String scope, String key) {
        return internalValue(scope, key, String.class, new HashSet<>());
    }

    @JsonIgnore
    public <T> Optional<T> getValue(String key, Class<T> clazz) {
        return getValue(GLOBAL_SCOPE, key, clazz);
    }

    @JsonIgnore
    public <T> Optional<T> getValue(String scope, String key, Class<T> clazz) {
        return internalValue(scope, key, clazz, new HashSet<>());
    }

    @JsonIgnore
    private <T> Optional<T> internalValue(String scope, String key, Class<T> clazz, Set<String> visitedKeys) {
        Map<String, Serializable> scopeEntries = getScopeEntries(scope);
        if (!scopeEntries.containsKey(key)) {
            scopeEntries = getScopeEntries(GLOBAL_SCOPE);
        }
        Object value = scopeEntries.get(key);
        if (value == null || clazz == null) {
            return Optional.empty();
        }
        if (value instanceof String && ((String) value).startsWith(VAR_PREFIX)) {
            // Value is a variable, go to the target variable (include loop detection)
            String targetKey = ((String) value).substring(VAR_PREFIX.length());
            if (visitedKeys.contains(targetKey)) {
                LOGGER.log(Level.WARNING, "Detected variable reference loop for key: " + key);
                return Optional.empty();
            }
            visitedKeys.add(targetKey);
            return internalValue(scope, targetKey, clazz, visitedKeys);
        }
        if (clazz.isInstance(value)) {
            return Optional.of (clazz.cast(value));
        }
        return Optional.empty();
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
