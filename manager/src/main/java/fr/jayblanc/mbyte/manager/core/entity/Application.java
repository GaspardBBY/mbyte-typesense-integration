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
import fr.jayblanc.mbyte.manager.core.ApplicationStatus;
import jakarta.persistence.*;

/**
 * @author Jerome Blanchard
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "Application.findAll", query = "SELECT a FROM Application a"),
        @NamedQuery(name = "Application.findByOwner", query = "SELECT a FROM Application a WHERE a.owner = :owner"),
        @NamedQuery(name = "Application.findByOwnerAndType", query = "SELECT a FROM Application a WHERE a.owner = :owner AND a.type = :type"),
})
@Table(
        name = "app",
        indexes = {
        @Index(name = "app_idx", columnList = "owner"),
        @Index(name = "app_idx", columnList = "owner, type")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Application {

    @Id
    private String id;
    private String owner;
    private String name;
    private String type;
    private long creationDate;
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    public Application() {
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

}
