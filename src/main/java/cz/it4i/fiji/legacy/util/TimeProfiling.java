package cz.it4i.fiji.legacy.util;

public class TimeProfiling {
	public static long tic() {
		return System.currentTimeMillis();
	}

	/** returns delta time between the current time and the given one from tic() */
	public static long tac(final long sourceTime) {
		return System.currentTimeMillis()-sourceTime;
	}

	/** converts given delta time to report the time in seconds */
	public static double seconds(final long deltaTime) {
		return (deltaTime/1000.0);
	}


	/** reports 'msg'+" took XX seconds" and returns new tic() */
	public static long tactic(final long sourceTime, final String msg) {
		System.out.println(msg+" took "
				+((System.currentTimeMillis()-sourceTime)/1000)+" seconds");
		return System.currentTimeMillis();
	}

}
