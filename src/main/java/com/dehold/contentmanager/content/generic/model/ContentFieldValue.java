package com.dehold.contentmanager.content.generic.model;

import java.util.Objects;

public class ContentFieldValue {
    private final String name;
    private final ValueType valueType;
    private final Object value; // Holds the typed value

    public ContentFieldValue(String name, ValueType valueType, Object value) {
        this.name = name;
        this.valueType = valueType;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public Object getValue() {
        return value;
    }

    public String asString() {
        return valueType == ValueType.STRING ? (String) value : String.valueOf(value);
    }

    public Integer asInteger() {
        return (Integer) value;
    }

    public Boolean asBoolean() {
        return (Boolean) value;
    }

    public Double asDouble() {
        return ((Number) value).doubleValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContentFieldValue that)) return false;
        return Objects.equals(name, that.name) &&
                valueType == that.valueType &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, valueType, value);
    }
}
