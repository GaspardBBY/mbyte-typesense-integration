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
    public void testProcessContextLog() {
        ProcessContext context = new ProcessContext();
        context.appendLog("First log entry.\n");
        context.appendLog("Second log entry.\n");
        String log = context.getLogger().toString();
        assertTrue(log.contains("First log entry."));
        assertTrue(log.contains("Second log entry."));
    }

    @Test
    public void testProcessContextEntries() {
        ProcessContext context = new ProcessContext();
        context.setValue("task1", "key1", "value1");
        context.setValue("task2", "key1", 42);
        context.setValue("key2", "value2");

        assertEquals("value1", context.getStringValue("task1", "key1"));
        assertEquals(42, context.getValue("task2", "key1", Integer.class));
        assertNull(context.getValue("task2", "key2", Integer.class));
        assertNull(context.getValue("task1", "key2", Integer.class));
        assertNotNull(context.getStringValue("key2"));
        assertEquals("value2", context.getStringValue("key2"));
        assertEquals("value2", context.getStringValue("task1", "key2"));
        assertEquals("value2", context.getStringValue("task2", "key2"));
    }

}
