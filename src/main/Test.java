package main;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import json.JsonClass;
import json.JsonClass.Type;
import json.JsonField;
import json.JsonField.GenType;
import json.JsonDecoder;
import json.JsonDecoder.OnInjected;
import json.JsonEncoder;
import json.JsonException;

public class Test {

	@JsonClass(type = Type.DATA)
	public static class JsonA {

		public static JsonB gen(JsonA obj, String tag, JsonElement jobj) {
			return new JsonB(obj);
		}

		@JsonField(generic = Integer.class)
		public final ArrayList<Integer> f0 = null;

		@JsonField()
		public JsonC f1;

		@JsonField(GenType = GenType.GEN, generator = "gen", generic = JsonB.class)
		public ArrayList<JsonB> f2;

		@JsonField(GenType = GenType.FILL)
		public JsonB f3 = new JsonB(this);

		@JsonField(generic = { Integer.class, String.class })
		public HashMap<Integer, String> f4 = null;

		@JsonField
		public JsonD data;

	}

	@JsonClass(type = Type.FILL)
	public static class JsonB {

		public JsonA par;

		@JsonField(generic = Integer.class)
		public HashSet<Integer> f;

		public JsonB(JsonA a) {
			par = a;
		}

		@OnInjected
		public void create() {
			System.out.println("OnInjected: " + f.size());
		}

	}

	@JsonClass(type = Type.MANUAL, generator = "gen")
	public static class JsonC {

		public static JsonC gen(JsonObject o) throws JsonException {
			return new JsonC();
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

	@JsonClass(type = Type.ALLDATA)
	public static class JsonD {

		public int a;

		public int[] b;

		public String c;

		public String[] d;

		public boolean e;

	}

	public static void main(String[] args) throws Exception {
		//PackLoader.writePack(new File("./pack.pack"), new File("./src"), "ver", "id", "test", "password");
		//PackLoader.readPack((str) -> getFile(new File("./out/" + str)), new File("./pack.pack"));
		File f = new File("./test.json");
		JsonElement elem = JsonParser.parseReader(new FileReader(f));
		JsonA obj = JsonDecoder.decode(elem, JsonA.class);
		System.out.println(JsonEncoder.encode(obj));
	}

	private static File getFile(File f) {
		try {
			if (!f.getParentFile().exists())
				f.getParentFile().mkdirs();
			if (!f.exists())
				f.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return f;
	}

}
