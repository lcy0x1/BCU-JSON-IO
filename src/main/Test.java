package main;

import java.io.File;
import java.io.FileReader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import json.JsonClass;
import json.JsonClass.Type;
import json.JsonField;
import json.JsonDecoder;
import json.JsonDecoder.OnInjected;
import json.JsonEncoder;
import json.JsonException;

public class Test {

	@JsonClass(type = Type.DATA)
	public static class JsonA {

		public static JsonB gen(JsonA obj, JsonObject jobj) {
			return new JsonB(obj);
		}

		@JsonField()
		public final int[] f0 = null;

		@JsonField()
		public JsonC f1;

		@JsonField(GenType = JsonField.GenType.GEN, generator = "gen")
		public JsonB[] f2;

		@JsonField(GenType = JsonField.GenType.FILL)
		public JsonB f3 = new JsonB(this);

	}

	@JsonClass(type = Type.FILL)
	public static class JsonB {

		public JsonA par;

		@JsonField
		public int[] f;

		public JsonB(JsonA a) {
			par = a;
		}

		@OnInjected
		public void create() {
			System.out.println("OnInjected: " + f.length);
		}

	}

	@JsonClass(type = Type.MANUAL, generator = "gen")
	public static class JsonC {

		public static JsonC gen(JsonObject o) throws JsonException {
			return (JsonC) JsonDecoder.inject(o, JsonC.class, new JsonC());
		}

		@JsonField(tag = "a", IOType = JsonField.IOType.W)
		public int getA() {
			return 10;
		}

		@JsonField(tag = "a", IOType = JsonField.IOType.R)
		public void setA(int a) {
			System.out.println(a);
		};

	}

	public static void main(String[] args) throws Exception {
		File f = new File("./test.json");
		JsonElement elem = JsonParser.parseReader(new FileReader(f));
		JsonA obj = JsonDecoder.decode(elem, JsonA.class);
		System.out.println(JsonEncoder.encode(obj));
	}

}
