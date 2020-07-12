package json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import json.JsonException.Type;

public class JsonEncoder {

	public static JsonElement encode(Object obj) throws Exception {
		if (obj == null)
			return JsonNull.INSTANCE;
		if (obj instanceof JsonElement)
			return (JsonElement) obj;
		if (obj instanceof Number)
			return new JsonPrimitive((Number) obj);
		if (obj instanceof Boolean)
			return new JsonPrimitive((Boolean) obj);
		if (obj instanceof String)
			return new JsonPrimitive((String) obj);
		Class<?> cls = obj.getClass();
		if (cls.isArray()) {
			int n = Array.getLength(obj);
			JsonArray arr = new JsonArray(n);
			for (int i = 0; i < n; i++)
				arr.add(encode(Array.get(obj, i)));
			return arr;
		}
		if (cls.getAnnotation(JsonClass.class) != null)
			return encodeObject(new JsonObject(), obj, cls);
		if(obj instanceof List)
			return encodeList((List<?>) obj);
		if(obj instanceof Set)
			return encodeSet((Set<?>) obj);
		if(obj instanceof Map)
			return encodeMap((Map<?,?>) obj);
		throw new JsonException(Type.UNDEFINED, null, "object " + obj + " not defined");
	}

	private static JsonArray encodeList(List<?> list) throws Exception {
		JsonArray ans = new JsonArray(list.size());
		for (Object obj : list)
			ans.add(encode(obj));
		return ans;
	}

	private static JsonArray encodeSet(Set<?> set) throws Exception {
		JsonArray ans = new JsonArray(set.size());
		for (Object obj : set)
			ans.add(encode(obj));
		return ans;
	}

	private static JsonArray encodeMap(Map<?, ?> map) throws Exception {
		JsonArray ans = new JsonArray(map.size());
		for (Entry<?, ?> obj : map.entrySet()) {
			JsonObject ent = new JsonObject();
			ent.add("key", encode(obj.getKey()));
			ent.add("val", encode(obj.getValue()));
			ans.add(ent);
		}
		return ans;
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
