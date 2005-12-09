package uk.co.uwcs.choob.support;

import java.lang.reflect.*;

public interface ObjectDBObject {
	/**
	 * Gets the class name of the object in a ObjectDB-compatible format.
	 * @returns The fully qualified class name (e.g. "plugin.MyPlugin.SomeClass").
	 */
	String getClassName();
	
	/**
	 * Gets the unique ID value for the object. The ID identifies the object in the DB.
	 */
	int getId();
	
	/**
	 * Sets the unique ID value for the object. The ID identifies the object in the DB.
	 */
	void setId(int id);
	
	/**
	 * Gets all the fields that should be saved to the ObjectDB.
	 * @returns An array of Strings containing the name of each property/field to save.
	 */
	String[] getFields();
	
	/**
	 * Gets the type of a single property/field.
	 * @returns Returns a Java TYPE class, either Boolean.TYPE, String.class, or some decendant of Number.
	 */
	Type getFieldType(String name) throws NoSuchFieldException;
	
	/**
	 * Gets the value of a single property/field.
	 * @returns The value contained in the property, as an Object. Can be typecast to whatever getFieldType returned.
	 */
	Object getFieldValue(String name) throws NoSuchFieldException, IllegalAccessException;
	
	/**
	 * Sets the value of a single property/field. The type of the value should match the type returned by getFieldType.
	 */
	void setFieldValue(String name, Object value) throws NoSuchFieldException, IllegalAccessException;
}