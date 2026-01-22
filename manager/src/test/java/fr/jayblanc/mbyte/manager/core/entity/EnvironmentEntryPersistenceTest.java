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

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class EnvironmentEntryPersistenceTest {

    @Inject
    EntityManager em;

    @Test
    @Transactional
    void shouldReloadSameDynamicType_string() {
        Environment env = Environment.of("app1", EnvironmentEntry.of("k", "hello", false));
        String envid = env.getId();
        em.persist(env);
        em.flush();
        em.clear();

        Environment reloaded = em.find(Environment.class, envid);
        assertNotNull(reloaded);
        assertEquals(1, reloaded.listEntries().size());

        EnvironmentEntry reloadedProp = reloaded.get("k");
        Object value = reloadedProp.getValue();
        assertInstanceOf(String.class, value);
        assertEquals("hello", value);
        assertEquals(String.class.getName(), reloadedProp.getPayloadType());
        assertEquals("hello", reloadedProp.getPayload());
    }

    @Test
    @Transactional
    void shouldReloadSameDynamicType_int() {
        Environment env = Environment.of("app2", EnvironmentEntry.of("k", 42, false));
        String envid = env.getId();
        em.persist(env);
        em.flush();
        em.clear();

        Environment reloaded = em.find(Environment.class, envid);
        EnvironmentEntry reloadedProp = reloaded.get("k");

        Object value = reloadedProp.getValue();
        assertInstanceOf(Integer.class, value);
        assertEquals(42, value);
        assertEquals(Integer.class.getName(), reloadedProp.getPayloadType());
        assertEquals("42", reloadedProp.getPayload());
    }

    @Test
    @Transactional
    void shouldReloadSameDynamicType_long() {
        Environment env = Environment.of("app3", EnvironmentEntry.of("k", 42L, false));
        String envid = env.getId();
        em.persist(env);
        em.flush();
        em.clear();

        Environment reloaded = em.find(Environment.class, envid);
        EnvironmentEntry reloadedProp = reloaded.get("k");

        Object value = reloadedProp.getValue();
        assertInstanceOf(Long.class, value);
        assertEquals(42L, value);
        assertEquals(Long.class.getName(), reloadedProp.getPayloadType());
        assertEquals("42", reloadedProp.getPayload());
    }

    @Test
    @Transactional
    void shouldReloadSameDynamicType_float() {
        Environment env = Environment.of("app4", EnvironmentEntry.of("k", 1.5f, false));
        String envid = env.getId();
        em.persist(env);
        em.flush();
        em.clear();

        Environment reloaded = em.find(Environment.class, envid);
        EnvironmentEntry reloadedProp = reloaded.get("k");

        Object value = reloadedProp.getValue();
        assertInstanceOf(Float.class, value);
        assertEquals(1.5f, (Float) value, 0.00001f);
        assertEquals(Float.class.getName(), reloadedProp.getPayloadType());
        assertEquals("1.5", reloadedProp.getPayload());
    }

    @Test
    @Transactional
    void shouldReloadSameDynamicType_double() {
        Environment env = Environment.of("app5", EnvironmentEntry.of("k", 2.5d, false));
        String envid = env.getId();
        em.persist(env);
        em.flush();
        em.clear();

        Environment reloaded = em.find(Environment.class, envid);
        EnvironmentEntry reloadedProp = reloaded.get("k");

        Object value = reloadedProp.getValue();
        assertInstanceOf(Double.class, value);
        assertEquals(2.5d, (Double) value, 0.0000001d);
        assertEquals(Double.class.getName(), reloadedProp.getPayloadType());
        assertEquals("2.5", reloadedProp.getPayload());
    }

    @Test
    @Transactional
    void shouldPersistSecretPrimitiveAsBase64() {
        Environment env = Environment.of("app6", EnvironmentEntry.of("pwd", "super-secret", true));
        String envid = env.getId();
        em.persist(env);
        em.flush();
        em.clear();

        Environment reloaded = em.find(Environment.class, envid);
        EnvironmentEntry reloadedProp = reloaded.get("pwd");

        Object value = reloadedProp.getValue();
        assertInstanceOf(String.class, value);
        assertEquals("super-secret", value);
        assertEquals(String.class.getName(), reloadedProp.getPayloadType());
        assertNotNull(reloadedProp.getPayload());
        assertNotEquals("super-secret", reloadedProp.getPayload());
    }

    @Test
    @Transactional
    void shouldSerializeNonPrimitiveWithObjectOutputStream() {
        Serializable obj = new DummySerializable("abc", 7);
        Environment env = Environment.of("app7", EnvironmentEntry.of("obj", obj, false));
        String envid = env.getId();
        em.persist(env);
        em.flush();
        em.clear();

        Environment reloaded = em.find(Environment.class, envid);
        EnvironmentEntry reloadedProp = reloaded.get("obj");

        Object value = reloadedProp.getValue();
        assertInstanceOf(DummySerializable.class, value);
        assertEquals(obj, value);
        assertEquals(DummySerializable.class.getName(), reloadedProp.getPayloadType());
        assertNotNull(reloadedProp.getPayload());
    }

    private record DummySerializable(String a, int b) implements Serializable {
    }
}
