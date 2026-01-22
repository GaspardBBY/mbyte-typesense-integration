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
package fr.jayblanc.mbyte.manager.core.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.*;

/**
 * @author Jerome Blanchard
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "Environment.findAll", query = "SELECT e FROM Environment e"),
        @NamedQuery(name = "Environment.findByApp", query = "SELECT e FROM Environment e WHERE e.app = :app")
})
@Table(
        name = "env",
        indexes = {
            @Index(name = "env_idx", columnList = "app")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Environment {

    @Id
    private String id;
    private String app;
    @Transient
    private Map<String, EnvironmentEntry> entries = new HashMap<>();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "env_entries", joinColumns = @JoinColumn(name = "env_id"))
    private Set<EnvironmentEntry> persistedEntries = new HashSet<>();

    public Environment() {
        this.id = UUID.randomUUID().toString();
    }

    public Environment(String app) {
        this();
        this.app = app;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public boolean contains(String key) {
        return this.entries.containsKey(key);
    }

    public EnvironmentEntry add(EnvironmentEntry environmentEntry) {
        EnvironmentEntry toPut = environmentEntry;
        EnvironmentEntry existing = persistedEntries.stream().filter(e -> e.getKey().equals(environmentEntry.getKey())).findFirst().orElse(null);
        if (existing != null) {
            existing.setValue(environmentEntry.getValue());
            existing.onPersist();
            toPut = existing;
        } else {
            environmentEntry.onPersist();
            persistedEntries.add(environmentEntry);
        }
        return this.entries.put(environmentEntry.getKey(), toPut);
    }

    public void addAll(Collection<EnvironmentEntry> environmentEntries) {
        for (EnvironmentEntry entry : environmentEntries) {
            this.add(entry);
        }
    }

    public EnvironmentEntry get(String key) {
        return this.entries.get(key);
    }

    public EnvironmentEntry addSecretEntry(String key, Serializable value) {
        return this.add(new EnvironmentEntry(key, value, true));
    }

    public EnvironmentEntry addEntry(String key, Serializable value) {
        return this.add(new EnvironmentEntry(key, value, false));
    }

    public Collection<EnvironmentEntry> listEntries() {
        return this.entries.values();
    }

    @PrePersist
    @PreUpdate
    void onPersist() {
        persistedEntries.clear();
        for (EnvironmentEntry entry : entries.values()) {
            entry.onPersist();
            persistedEntries.add(entry);
        }
    }

    @PostLoad
    void onLoad() {
        entries.clear();
        for (EnvironmentEntry entry : persistedEntries) {
            entry.onLoad();
            entries.put(entry.getKey(), entry);
        }
    }

    public static Environment of(String app, EnvironmentEntry entry) {
        Environment env = new Environment(app);
        env.add(entry);
        return env;
    }

    public static Environment of(String app, EnvironmentEntry... entries) {
        Environment env = new Environment(app);
        for (EnvironmentEntry entry : entries) {
            env.add(entry);
        }
        return env;
    }

    public static Environment of(String app, Set<EnvironmentEntry> entries) {
        Environment env = new Environment(app);
        env.addAll(entries);
        return env;
    }

    public static Environment of(String app) {
        return new Environment(app);
    }

}
