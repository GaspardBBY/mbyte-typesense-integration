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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerome Blanchard
 */
public class ProcessContextTest {

    @Test
    public void testProcessContextEntries() {
        ProcessContext context = new ProcessContext();
        context.setValue("task1", "key1", "value1");
        context.setValue("task2", "key1", 42);
        context.setValue("key2", "value2");

        assertTrue(context.getStringValue("task1", "key1").isPresent());
        assertEquals("value1", context.getStringValue("task1", "key1").get());
        assertTrue(context.getValue("task2", "key1", Integer.class).isPresent());
        assertEquals(42, context.getValue("task2", "key1", Integer.class).get());
        assertFalse(context.getValue("task2", "key2", Integer.class).isPresent());
        assertFalse(context.getValue("task1", "key2", Integer.class).isPresent());
        assertTrue(context.getStringValue("key2").isPresent());
        assertEquals("value2", context.getStringValue("key2").get());
        assertTrue(context.getStringValue("task1", "key2").isPresent());
        assertEquals("value2", context.getStringValue("task1", "key2").get());
        assertTrue(context.getStringValue("task2", "key2").isPresent());
        assertEquals("value2", context.getStringValue("task2", "key2").get());
    }

    @Test
    public void testProcessContextEntriesWithVar() {
        ProcessContext context = new ProcessContext();
        context.setValue( "global_key", "global_value");
        context.setValue("task1", "key", "$global_key");
        context.setValue("task2", "key", "$global_key");

        assertTrue(context.getStringValue( "global_key").isPresent());
        assertEquals("global_value", context.getStringValue( "global_key").get());
        assertTrue(context.getStringValue( "task1", "global_key").isPresent());
        assertEquals("global_value", context.getStringValue( "task1", "global_key").get());
        assertTrue(context.getStringValue( "task2", "global_key").isPresent());
        assertEquals("global_value", context.getStringValue( "task2", "global_key").get());
    }

    @Test
    public void testProcessContextEntriesWithVarLoop() {
        ProcessContext context = new ProcessContext();
        context.setValue( "global_key", "global_value");
        context.setValue( "loop_key", "$global_key");
        context.setValue("task1", "key", "$loop_key");
        context.setValue("task2", "global_key", "$loop_key");

        assertTrue(context.getStringValue("task1", "key").isPresent());
        assertEquals("global_value", context.getStringValue("task1", "key").get());

        assertFalse(context.getStringValue("task2", "loop_key").isPresent());
    }

}
