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

@Entity
@NamedQueries({
    @NamedQuery(name = "Store.findIdByOwner", query = "SELECT s.id FROM Store s WHERE s.owner = :owner")
})
@Table(indexes = {
        @Index(name = "stores_idx", columnList = "owner")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Store {

    @Id
    private String id;
    private String type;
    private String owner;
    private String name;
    private long creationDate;
    private float usage;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Transient
    private String location;

    public Store() {
    }

    public Store(String id, String type, String owner, String name) {
        this.id = id;
        this.type = type;
        this.owner = owner;
        this.name = name;
        this.creationDate = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public float getUsage() {
        return usage;
    }

    public void setUsage(float usage) {
        this.usage = usage;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public enum Status {
        CREATED,
        STARTING,
        AVAILABLE,
        LOST,
        ERROR
    }
}
