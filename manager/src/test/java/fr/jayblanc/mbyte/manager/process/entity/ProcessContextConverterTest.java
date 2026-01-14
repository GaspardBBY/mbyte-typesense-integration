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
class ProcessContextConverterTest {

    @Test
    void shouldSerializeAndDeserializeLogAndEntries() {
        ProcessContext context = new ProcessContext();
        context.appendLog("First log entry.\n");
        context.appendLog("Second log entry.\n");
        context.setValue("task1", "key1", "value1");
        context.setValue("task2", "key1", 42);
        context.setValue("key2", "value2");

        ProcessContextConverter converter = new ProcessContextConverter();

        String json = converter.convertToDatabaseColumn(context);
        assertNotNull(json);

        ProcessContext reconstructed = converter.convertToEntityAttribute(json);
        assertNotNull(reconstructed);

        // log is stored via the "logger" property (StringBuilder) and should survive the round-trip
        String reconstructedLog = reconstructed.getLogger().toString();
        assertTrue(reconstructedLog.contains("First log entry."));
        assertTrue(reconstructedLog.contains("Second log entry."));

        // entries should survive the round-trip and remain accessible via the public getters
        assertEquals("value1", reconstructed.getStringValue("task1", "key1"));
        assertEquals(42, reconstructed.getValue("task2", "key1", Integer.class));
        assertEquals("value2", reconstructed.getStringValue("key2"));
        assertEquals("value2", reconstructed.getStringValue("task1", "key2"));
        assertEquals("value2", reconstructed.getStringValue("task2", "key2"));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionOnInvalidJson() {
        ProcessContextConverter converter = new ProcessContextConverter();
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute("{not-json"));
    }
}

