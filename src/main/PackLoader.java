package main;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import json.JsonClass;
import json.JsonDecoder;
import json.JsonException;
import json.JsonField;

public class PackLoader {

	@JsonClass(type = JsonClass.Type.DATA)
	public static class PackDescriptor {

		@JsonClass(type = JsonClass.Type.DATA)
		public static class FileDescriptor {

			@JsonField
			public final String path;

			@JsonField
			public final int size;

			@JsonField
			public final boolean isTextFile;

			public FileDescriptor() {
				path = null;
				size = 0;
				isTextFile = false;
			}

			public FileDescriptor(String path, int size, boolean isText) {
				this.path = path;
				this.size = size;
				isTextFile = isText;
			}

		}

		@JsonField
		public final String BCU_VERSION = null;

		@JsonField
		public final String uuid = null;

		@JsonField
		public final FileDescriptor[] files = null;

		private void load(FileLoader loader) throws IOException {
			for (FileDescriptor fd : files) {
				if (fd.isTextFile) {
					byte[] data = new byte[fd.size];
					loader.fis.read(data);
					data = loader.decode(data);
					// TODO save file
				} else {
					int rem = fd.size;
					byte[] data = null;
					String path = ""; // TODO specify path
					FileOutputStream fos = new FileOutputStream(new File(path));
					while (rem > 0) {
						int size = Math.min(rem, CHUNK);
						if (data == null || data.length != size)
							data = new byte[size];
						loader.fis.read(data);
						fos.write(data);
					}
					fos.close();
				}
			}
		}

	}

	private static class FileLoader {

		private final FileInputStream fis;

		private final byte[] password;
		private final PackDescriptor pack;

		public FileLoader(File f) throws IOException, JsonException {
			fis = new FileInputStream(f);
			byte[] head = new byte[HEADER];
			fis.read(head);
			password = new byte[PASSWORD];
			fis.read(password);
			byte[] desc = new byte[readInt()];
			fis.read(desc);
			desc = decode(desc);
			JsonElement je = JsonParser.parseReader(new InputStreamReader(new ByteArrayInputStream(desc)));
			pack = JsonDecoder.decode(je, PackDescriptor.class);
			pack.load(this);
			fis.close();
		}

		private byte[] decode(byte[] file) {
			return file;// TODO
		}

		private int readInt() throws IOException {
			byte[] len = new byte[4];
			fis.read(len);
			return DataIO.toInt(DataIO.translate(len), 0);
		}
	}

	public static final int HEADER = 16, PASSWORD = 16, CHUNK = 1 << 20;

	public PackDescriptor readPack(File f) throws IOException, JsonException {
		return new FileLoader(f).pack;
	}

}
