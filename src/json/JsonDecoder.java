package json;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import json.JsonException.Type;

public class JsonDecoder {

	@Documented
	@Retention(RUNTIME)
	@Target({ METHOD })
	public static @interface OnInjected {
	}

	/**
	 * parse the json element into object
	 * 
	 * @param elem   the json element to parse
	 * @param cls    the class of the target value
	 * @param cont   the class holding this value
	 * @param jfield the annotation of the target field
	 * @param field  the field object of the target field
	 */
	public static Object decode(JsonElement elem, Class<?> cls, Object cont, JsonField jfield, Field field)
			throws JsonException {
		if (cls == Boolean.TYPE || cls == Boolean.class)
			return getBoolean(elem);
		if (cls == Integer.TYPE || cls == Integer.class)
			return getInt(elem);
		if (cls == Double.TYPE || cls == Double.class)
			return getDouble(elem);
		if (cls == String.class)
			return getString(elem);
		if (cls.isArray()) {
			if (elem.isJsonNull())
				return null;
			if (!elem.isJsonArray())
				throw new JsonException(Type.TYPE_MISMATCH, elem, "this element is not array");
			JsonArray jarr = elem.getAsJsonArray();
			int n = jarr.size();
			Class<?> ccls = cls.getComponentType();
			Object arr = null;
			if (jfield != null && jfield.GenType() == JsonField.GenType.FILL) {
				if (field == null || cont == null)
					throw new JsonException(Type.TAG, elem, "no enclosing object");
				try {
					arr = field.get(cont);
				} catch (Exception e) {
					JsonException je = new JsonException(Type.INTERNAL, elem, "");
					je.initCause(e);
					throw je;
				}
			} else
				arr = Array.newInstance(ccls, n);
			for (int i = 0; i < n; i++)
				Array.set(arr, i, decode(jarr.get(i), ccls, cont, jfield, field));
			return arr;
		}
		// TODO list and map
		JsonClass jc = cls.getAnnotation(JsonClass.class);
		if (jc != null) {
			if (elem.isJsonNull())
				return null;
			if (!elem.isJsonObject())
				throw new JsonException(Type.TYPE_MISMATCH, elem, "this element is not object");
			JsonObject jobj = elem.getAsJsonObject();
			if (jc.type() == JsonClass.Type.DATA) {
				try {
					return inject(jobj, cls, cls.newInstance());
				} catch (Exception e) {
					if (e instanceof JsonException)
						throw (JsonException) e;
					JsonException je = new JsonException(Type.INTERNAL, elem, "");
					je.initCause(e);
					throw je;
				}
			} else if (jc.type() == JsonClass.Type.FILL) {
				if (cont == null || jfield == null || jfield.GenType() == JsonField.GenType.SET
						|| jfield.GenType() == JsonField.GenType.GEN && jfield.generator().length() == 0)
					throw new JsonException(Type.FUNC, elem, "no generator parameter");
				Class<?> ccls = cont.getClass();
				try {
					Object val = null;
					if (jfield.GenType() == JsonField.GenType.GEN) {
						Method m = ccls.getDeclaredMethod(jfield.generator(), ccls, JsonObject.class);
						val = m.invoke(null, cont, jobj);
						if (!cls.isInstance(val))
							throw new JsonException(Type.FUNC, elem, "wrong return type");
					}
					if (jfield.GenType() == JsonField.GenType.FILL) {
						if (field == null)
							throw new JsonException(Type.TAG, null, "GenType FILL requires field");
						val = field.get(cont);
					}
					return inject(jobj, cls, val);
				} catch (Exception e) {
					if (e instanceof JsonException)
						throw (JsonException) e;
					JsonException je = new JsonException(Type.INTERNAL, elem, "");
					je.initCause(e);
					throw je;
				}
			} else if (jc.type() == JsonClass.Type.MANUAL) {
				String func = jc.generator();
				if (func.length() == 0)
					throw new JsonException(Type.FUNC, elem, "no generate function");
				try {
					Method m = cls.getDeclaredMethod(func, JsonObject.class);
					Object val = m.invoke(null, jobj);
					if (!cls.isInstance(val))
						throw new JsonException(Type.FUNC, elem, "wrong return type");
					return val;
				} catch (Exception e) {
					if (e instanceof JsonException)
						throw (JsonException) e;
					JsonException je = new JsonException(Type.INTERNAL, elem, "");
					je.initCause(e);
					throw je;
				}
			}
		}
		throw new JsonException(Type.UNDEFINED, elem, "class not possible to generate");
	}

	@SuppressWarnings("unchecked")
	public static <T> T decode(JsonElement elem, Class<T> cls) throws JsonException {
		return (T) decode(elem, cls, null, null, null);
	}

	public static boolean getBoolean(JsonElement elem) throws JsonException {
		if (!elem.isJsonPrimitive() || !((JsonPrimitive) elem).isBoolean())
			throw new JsonException(Type.TYPE_MISMATCH, elem, "this element is not boolean");
		return elem.getAsBoolean();
	}

	public static double getDouble(JsonElement elem) throws JsonException {
		if (!elem.isJsonPrimitive() || !((JsonPrimitive) elem).isNumber())
			throw new JsonException(Type.TYPE_MISMATCH, elem, "this element is not number");
		return elem.getAsDouble();
	}

	public static int getInt(JsonElement elem) throws JsonException {
		if (!elem.isJsonPrimitive() || !((JsonPrimitive) elem).isNumber())
			throw new JsonException(Type.TYPE_MISMATCH, elem, "this element is not number");
		return elem.getAsInt();
	}

	public static String getString(JsonElement elem) throws JsonException {
		if (elem.isJsonNull())
			return null;
		if (!elem.isJsonPrimitive() || !((JsonPrimitive) elem).isString())
			throw new JsonException(Type.TYPE_MISMATCH, elem, "this element is not string");
		return elem.getAsString();
	}

	/**
	 * inject the values from the json object into the target object
	 */
	public static Object inject(JsonObject jobj, Class<?> cls, Object obj) throws JsonException {
		JsonClass jc = cls.getAnnotation(JsonClass.class);
		if (jc == null)
			throw new JsonException(Type.TYPE_MISMATCH, jobj, "no annotation for class " + cls);
		if (cls.getSuperclass().getAnnotation(JsonClass.class) != null)
			inject(jobj, cls.getSuperclass(), obj);
		Field[] fs = cls.getDeclaredFields();
		for (Field f : fs) {
			JsonField jf = f.getAnnotation(JsonField.class);
			if (jf == null || jf.IOType() == JsonField.IOType.W)
				continue;
			try {
				String tag = jf.tag();
				if (tag.length() == 0)
					tag = f.getName();
				if (!jobj.has(tag))
					continue;
				JsonElement elem = jobj.get(tag);
				f.setAccessible(true);
				f.set(obj, decode(elem, f.getType(), obj, jf, f));
			} catch (Exception e) {
				if (jf.noErr())
					continue;
				if (e instanceof JsonException)
					throw (JsonException) e;
				JsonException je = new JsonException(Type.INTERNAL, null, "");
				je.initCause(e);
				throw je;
			}
		}
		Method oni = null;
		for (Method m : cls.getDeclaredMethods()) {
			if (m.getAnnotation(OnInjected.class) != null)
				if (oni == null)
					oni = m;
				else
					throw new JsonException(Type.FUNC, null, "duplicate OnInjected");
			JsonField jf = m.getAnnotation(JsonField.class);
			if (jf == null || jf.IOType() == JsonField.IOType.W)
				continue;
			if (jf.IOType() == JsonField.IOType.RW)
				throw new JsonException(Type.FUNC, null, "functional fields should not have RW type");
			if (m.getParameterCount() != 1)
				throw new JsonException(Type.FUNC, null, "parameter count should be 1");
			String tag = jf.tag();
			if (tag.length() == 0)
				throw new JsonException(Type.TAG, null, "function fields must have tag");
			if (!jobj.has(tag))
				continue;
			JsonElement elem = jobj.get(tag);
			Class<?> ccls = m.getParameters()[0].getType();
			try {
				m.invoke(obj, decode(elem, ccls, obj, jf, null));
			} catch (Exception e) {
				if (jf.noErr())
					continue;
				if (e instanceof JsonException)
					throw (JsonException) e;
				JsonException je = new JsonException(Type.INTERNAL, null, "");
				je.initCause(e);
				throw je;
			}
		}
		if (oni != null)
			try {
				oni.invoke(obj);
			} catch (Exception e) {
				if (e instanceof JsonException)
					throw (JsonException) e;
				JsonException je = new JsonException(Type.INTERNAL, null, "");
				je.initCause(e);
				throw je;
			}
		return obj;
	}

}
