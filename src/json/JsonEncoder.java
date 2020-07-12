package json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import json.JsonException.Type;

public class JsonEncoder {

	public static JsonElement encode(Object obj) throws Exception {
		if (obj instanceof Number)
			return new JsonPrimitive((Number) obj);
		if (obj instanceof Boolean)
			return new JsonPrimitive((Boolean) obj);
		if (obj instanceof String)
			return new JsonPrimitive((String) obj);
		if (obj == null)
			return JsonNull.INSTANCE;
		Class<?> cls = obj.getClass();
		if (cls.isArray()) {
			int n = Array.getLength(obj);
			JsonArray arr = new JsonArray(n);
			for (int i = 0; i < n; i++)
				arr.add(encode(Array.get(obj, i)));
			return arr;
		}
		// TODO list and map
		if (cls.getAnnotation(JsonClass.class) != null)
			return encodeObject(new JsonObject(), obj, cls);
		throw new JsonException(Type.UNDEFINED, null, "object " + obj + " not defined");
	}

	private static JsonObject encodeObject(JsonObject jobj, Object obj, Class<?> cls) throws Exception {
		if (cls.getSuperclass().getAnnotation(JsonClass.class) != null)
			encodeObject(jobj, obj, cls);
		for (Field f : cls.getDeclaredFields())
			if (f.getAnnotation(JsonField.class) != null) {
				JsonField jf = f.getAnnotation(JsonField.class);
				if (jf.IOType() == JsonField.IOType.R)
					continue;
				String tag = jf.tag().length() == 0 ? f.getName() : jf.tag();
				jobj.add(tag, encode(f.get(obj)));
			}
		for (Method m : cls.getDeclaredMethods())
			if (m.getAnnotation(JsonField.class) != null) {
				JsonField jf = m.getAnnotation(JsonField.class);
				if (jf.IOType() == JsonField.IOType.R)
					continue;
				if (jf.IOType() == JsonField.IOType.RW)
					throw new JsonException(Type.FUNC, null, "functional fields should not have RW type");
				String tag = jf.tag();
				if (tag.length() == 0)
					throw new JsonException(Type.TAG, null, "function fields must have tag");
				jobj.add(tag, encode(m.invoke(obj)));
			}
		return jobj;
	}

}
