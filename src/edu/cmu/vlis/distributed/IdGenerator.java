package edu.cmu.vlis.distributed;

public class IdGenerator {
	private static int cnt = 0;
	public static String generatorId() {
		return "T" + cnt++;
	}
}
