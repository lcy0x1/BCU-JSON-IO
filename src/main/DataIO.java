package main;

public strictfp abstract class DataIO {

	/** write a number n into a byte[] start from index */
	public static void fromByte(byte[] b, int index, byte n) {
		b[index] = n;
	}

	public static void fromInt(byte[] b, int index, int n) {
		for (int i = 0; i < 4; i++)
			b[index + i] = (byte) (n >> (i * 8) & 0xff);
	}

	public static int toInt(int[] datas, int index) {
		int ans = 0;
		for (int i = 0; i < 4; i++)
			ans += (datas[index + i]) << (i * 8);
		return ans;
	}

	public static int[] translate(byte[] datas) {
		int[] ans = new int[datas.length];
		for (int i = 0; i < ans.length; i++)
			ans[i] = (datas[i]) & 0xff;
		return ans;
	}

	public static byte[] translate(int[] datas) {
		byte[] ans = new byte[datas.length];
		for (int i = 0; i < ans.length; i++)
			ans[i] = (byte) datas[i];
		return ans;
	}

}