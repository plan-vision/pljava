/*
 * Copyright (c) 2004-2020 Tada AB and other contributors, as listed below.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the The BSD 3-Clause License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Contributors:
 *   Tada AB
 *   Chapman Flack
 */
package org.postgresql.pljava.example.annotation;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.postgresql.pljava.annotation.Function;
import static org.postgresql.pljava.annotation.Function.Effects.IMMUTABLE;
import static
	org.postgresql.pljava.annotation.Function.OnNullInput.RETURNS_NULL;
import org.postgresql.pljava.annotation.SQLAction;
import org.postgresql.pljava.annotation.SQLType;

/*
 * This SQLAction declares a function that refers to Java's own String.format
 * method, which is variadic, and tests its behavior.
 *
 * It has a 'provides' string, and makeArray below 'requires' it, because there
 * is a bug in the C Type caching that will make this test fail if the
 * makeArray function has been initialized first. (Until that bug is fixed, the
 * time is not ripe to outright advertise that variadic functions like this are
 * supported. But this test is here to ensure at least the support does not
 * regress.)
 *
 * The bug is that PL/Java treats java.lang.Object[] as anyarray, intended to be
 * adaptable to whatever array type is passed, but once the Type caching logic
 * has already associated the Oid for anyarray upon seeing the return type of
 * makeArray below, it cannot later supply a Type for text[] as seen in the
 * parameter list of format().
 */
@SQLAction(provides = "String.format tested",
	install = {
		"CREATE FUNCTION javatest.format(" +
		"  format pg_catalog.text," +
		"  VARIADIC args pg_catalog.text[])" +
		" RETURNS pg_catalog.text" +
		" LANGUAGE java" +
		" AS 'java.lang.String=" +
		"     java.lang.String.format(java.lang.String,java.lang.Object[])'",

		"SELECT" +
		"  CASE" +
		"   WHEN result OPERATOR(pg_catalog.=) 'Hello, world'" +
		"   THEN javatest.logmessage('INFO', 'variadic call ok')" +
		"   ELSE javatest.logmessage('WARNING', 'variadic call ng')" +
		"  END" +
		" FROM" +
		"  javatest.format('Hello, %s', 'world') AS result"
	},
	remove = "DROP FUNCTION javatest.format(pg_catalog.text,pg_catalog.text[])"
)
/**
 * Provides example methods to illustrate the polymorphic types {@code any},
 * {@code anyarray}, and {@code anyelement}.
 */
public class AnyTest {
	private static Logger s_logger = Logger.getAnonymousLogger();

	/**
	 * Log (at INFO level) the Java class received for the passed argument.
	 */
	@Function(schema="javatest", effects=IMMUTABLE, onNullInput=RETURNS_NULL)
	public static void logAny(@SQLType("pg_catalog.any") Object param)
	throws SQLException
	{
		s_logger.info("logAny received an object of class " + param.getClass());
	}

	/**
	 * Log (at INFO level) the Java class received for the passed argument, and
	 * return the same value.
	 */
	@Function(schema="javatest", effects=IMMUTABLE, onNullInput=RETURNS_NULL,
		type="pg_catalog.anyelement")
	public static Object logAnyElement(
		@SQLType("pg_catalog.anyelement") Object param)
	throws SQLException
	{
		s_logger.info("logAnyElement received an object of class "
				+ param.getClass());
		return param;
	}

	/**
	 * Return the Java object received for the passed argument, wrapped in a
	 * one-element array with the object's class as its element type.
	 */
	@Function(schema="javatest", effects=IMMUTABLE, onNullInput=RETURNS_NULL,
		type="pg_catalog.anyarray", requires="String.format tested")
	public static Object[] makeArray(
		@SQLType("pg_catalog.anyelement") Object param)
	{
		Object[] result = (Object[]) Array.newInstance(param.getClass(), 1);
		result[0] = param;
		return result;
	}
}
