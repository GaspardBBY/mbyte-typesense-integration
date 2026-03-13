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
package fr.jayblanc.mbyte.store.index;

import java.util.LinkedHashMap;
import java.util.Map;

public class IndexStoreDocumentBuilder {

    public static final String ID_FIELD = "id";
    public static final String TYPE_FIELD = "type";
    public static final String SCOPE_FIELD = "scope";
    public static final String CONTENT_FIELD = "content";
    public static final String NAME_FIELD = "name";
    public static final String MIMETYPE_FIELD = "mimetype";
    public static final String NODE_TYPE_FIELD = "node_type";
    public static final String PARENT_FIELD = "parent";
    public static final String STORE_ID_FIELD = "store_id";
    public static final String MODIFIED_AT_FIELD = "modified_at";

    public static Map<String, Object> buildDocument(IndexableContent object) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put(ID_FIELD, object.getIdentifier());
        document.put(TYPE_FIELD, object.getType());
        document.put(SCOPE_FIELD, object.getScope().name());
        document.put(CONTENT_FIELD, object.getContent());
        document.put(NAME_FIELD, object.getName());
        document.put(MIMETYPE_FIELD, object.getMimetype());
        document.put(NODE_TYPE_FIELD, object.getNodeType());
        document.put(PARENT_FIELD, object.getParent());
        document.put(STORE_ID_FIELD, object.getStoreId());
        document.put(MODIFIED_AT_FIELD, object.getModifiedAt());
        return document;
    }

    private IndexStoreDocumentBuilder() {
    }
}
