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

import jakarta.persistence.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Embeddable
public class EnvironmentEntry {

    private static final List<Class<?>> BASIC_TYPES = List.of(
            String.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            Character.class
    );

    @Column(name = "env_key")
    private String key;
    @Transient
    private Serializable value;
    private String payload;
    private String payloadType;
    @Column(name = "env_secret")
    private boolean isSecret;

    public EnvironmentEntry() {
    }

    public EnvironmentEntry(String key, Serializable value) {
        this(key, value, false);
    }

    public EnvironmentEntry(String key, Serializable value, boolean isSecret) {
        this.key = key;
        this.isSecret = isSecret;
        this.value = value;
        this.payloadType = value.getClass().getName();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Serializable getValue() {
        return value;
    }

    public void setValue(Serializable value) {
        this.value = value;
        this.payloadType = value.getClass().getName();
    }

    public boolean isSecret() {
        return isSecret;
    }

    public void setSecret(boolean secret) {
        isSecret = secret;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(String payloadType) {
        this.payloadType = payloadType;
    }

    public static EnvironmentEntry of(String key, Serializable value) {
        return new EnvironmentEntry(key, value, false);
    };

    public static EnvironmentEntry of(String key, Serializable value, boolean isSecret) {
        return new EnvironmentEntry(key, value, isSecret);
    };

    void onPersist() {
        this.payload = toPayload(value, isSecret);
        this.payloadType = value.getClass().getName();
    }

    void onLoad() {
        this.value = fromPayload(payload, payloadType, isSecret);
    }

    private static String toPayload(Serializable value, boolean isSecret) {
        String payload = "";
        if (value == null) {
            return payload;
        }
        if (BASIC_TYPES.contains(value.getClass())) {
            payload = String.valueOf(value);
        } else {
            payload = base64(serializeJava(value));
        }
        return isSecret ? cipher(payload) : payload;
    }

    private static Serializable fromPayload(String payload, String type, boolean isSecret) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            Class valueClass = Class.forName(type);
            String effectivePayload = isSecret ? decipher(payload) : payload;
            if (!BASIC_TYPES.contains(valueClass)) {
                return deserializeJava(base64Decode(effectivePayload));
            }
            return switch (type) {
                case "java.lang.String" -> effectivePayload;
                case "java.lang.Integer" -> Integer.valueOf(effectivePayload);
                case "java.lang.Long" -> Long.valueOf(effectivePayload);
                case "java.lang.Float" -> Float.valueOf(effectivePayload);
                case "java.lang.Double" -> Double.valueOf(effectivePayload);
                case "java.lang.Character" -> effectivePayload.charAt(0);
                default -> throw new IllegalStateException("Unsupported basic type: " + type);
            };
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load StoreProperty value of type " + type, e);
        }
    }

    private static byte[] serializeJava(Serializable v) {
        Objects.requireNonNull(v, "value");
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(v);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to serialize StoreProperty value of type " + v.getClass().getName(), e);
        }
    }

    private static Serializable deserializeJava(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            Object obj = ois.readObject();
            return (Serializable) obj;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to deserialize StoreProperty value", e);
        }
    }

    // TODO ciphering must be implemented properly (not base64)
    private static String cipher(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String decipher(String b64) {
        return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
    }

    private static String base64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private static byte[] base64Decode(String b64) {
        return Base64.getDecoder().decode(b64);
    }

}
