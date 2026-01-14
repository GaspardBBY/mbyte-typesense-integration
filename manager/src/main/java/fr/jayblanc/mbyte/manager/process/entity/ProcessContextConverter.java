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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jerome Blanchard
 */
@Converter
public class ProcessContextConverter implements AttributeConverter<ProcessContext, String> {
    private static final Logger LOGGER = Logger.getLogger(ProcessContextConverter.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ProcessContext attribute) {
        try {
            String value = mapper.writeValueAsString(attribute);
            LOGGER.log(Level.FINEST, "Converting ProcessContext to Database: " + value);
            return value;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public ProcessContext convertToEntityAttribute(String dbData) {
        try {
            LOGGER.log(Level.FINEST, "Converting Database to ProcessContext: " + dbData);
            return mapper.readValue(dbData, ProcessContext.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
