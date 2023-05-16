package com.here.xyz.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A JSON class created from a Java class.
 */
@SuppressWarnings("unused")
public class JsonClass {

  private static final JsonField[] EMPTY = new JsonField[0];
  private static final ConcurrentHashMap<@NotNull Class<?>, @NotNull JsonClass> cache = new ConcurrentHashMap<>();

  /**
   * Returns a JSON class for a Java class. This method ignores private fields that are not annotated with {@link JsonProperty}.
   *
   * @param javaClass The JAVA class for which to return the JSON class.
   * @return the JSON class.
   */
  public static @NotNull JsonClass of(final @NotNull Class<?> javaClass) {
    JsonClass jsonClass = cache.get(javaClass);
    if (jsonClass != null) {
      return jsonClass;
    }
    jsonClass = new JsonClass(javaClass);
    final JsonClass existing = cache.putIfAbsent(javaClass, jsonClass);
    return existing != null ? existing : jsonClass;
  }

  /**
   * Returns a JSON class for a Java object, if the object is a class, the JSON class for this class returned, therefore this is the same as
   * calling {@link #of(Class)}. This method ignores private fields that are not annotated with {@link JsonProperty}.
   *
   * @param object The JAVA object for which to return the JSON class.
   * @return the JSON class.
   * @throws NullPointerException if given object is null.
   */
  public static @NotNull JsonClass of(final @NotNull Object object) {
    return of(object instanceof Class ? (Class<?>) object : object.getClass());
  }

  /**
   * Creates the JSON class from the given JAVA class.
   *
   * @param javaClass The JAVA class.
   */
  JsonClass(final @NotNull Class<?> javaClass) {
    this.javaClass = javaClass;
    JsonField[] jsonFields = EMPTY;
    Class<?> theClass = javaClass;
    while (theClass != null && theClass != Object.class) {
      final Field[] fields = theClass.getDeclaredFields();
      int new_fields_count = fields.length;
      for (int i = 0; i < fields.length; i++) {
        final Field field = fields[i];
        final JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (jsonProperty != null) {
          continue;
        }

        final JsonIgnore jsonIgnore = field.getAnnotation(JsonIgnore.class);
        final int fieldModifiers = field.getModifiers();
        if (jsonIgnore != null || Modifier.isStatic(fieldModifiers) || Modifier.isPrivate(fieldModifiers)) {
          fields[i] = null;
          new_fields_count--;
        }
      }
      if (new_fields_count > 0) {
        int newIndex = jsonFields.length;
        jsonFields = Arrays.copyOf(jsonFields, jsonFields.length + new_fields_count);
        for (final @Nullable Field field : fields) {
          if (field != null) {
            jsonFields[newIndex++] = new JsonField(this, field);
          }
        }
      }
      theClass = theClass.getSuperclass();
    }
    this.fields = jsonFields;
    for (final JsonField field : fields) {
      fieldsMap.putIfAbsent(field.jsonName, field);
    }
  }

  /**
   * All JSON fields of the class, those of the super class are at the end.
   *
   * <p><b>WARNING</b>: This array <b>MUST NOT</b> be modified, it is read-only!</p>
   */
  public final @NotNull JsonField @NotNull [] fields;

  /**
   * The JAVA class.
   */
  public final Class<?> javaClass;

  private final @NotNull LinkedHashMap<@NotNull String, @NotNull JsonField> fieldsMap = new LinkedHashMap<>();

  /**
   * Returns the field at the given index.
   *
   * @param index The index to query.
   * @return The JSON field.
   * @throws ArrayIndexOutOfBoundsException If the given index is out of bounds (less than zero or more than {@link #size()}.
   */
  public @NotNull JsonField getField(int index) {
    return fields[index];
  }

  /**
   * Returns the JSON field with the given name; if such a field exists.
   *
   * @param name The name of the field.
   * @return the JSON field; {@code null} if no such field exists.
   */
  public @Nullable JsonField getField(@Nullable CharSequence name) {
    if (name instanceof String key) {
      return fieldsMap.get(key);
    }
    return name != null ? fieldsMap.get(name.toString()) : null;
  }

  /**
   * Tests whether a property with the given name exists.
   *
   * @param name The name to test for.
   * @return {@code true} if such a property exists; {@code false} otherwise.
   */
  public boolean hasField(@Nullable CharSequence name) {
    if (name instanceof String) {
      return fieldsMap.containsKey((String) name);
    }
    return name != null && fieldsMap.containsKey(name.toString());
  }

  /**
   * Tests whether the given field index is valid.
   *
   * @param index The index to test for.
   * @return {@code true} if such a property exists; {@code false} otherwise.
   */
  public boolean hasField(int index) {
    return index >= 0 && index < fields.length;
  }

  /**
   * Returns the amount of fields.
   *
   * @return The amount of fields.
   */
  public int size() {
    return fields.length;
  }
}