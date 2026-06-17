package nl.tno.esdl.esdldrive.util;

public class OCLUtil {

	/***
	 * Escape a path with ' by \'
	 * @param path
	 * @return escaped path
	 */
	public static String escapeOCLPath(String path) {
		return path.replaceAll("'", "\\\\'");		
	}
	
}
