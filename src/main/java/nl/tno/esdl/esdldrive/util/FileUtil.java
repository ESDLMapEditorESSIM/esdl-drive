package nl.tno.esdl.esdldrive.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {
	
	public static void saveStringToFile(String s, String filename) throws IOException {
		Path path = Paths.get(filename);
		System.out.println("Writing to " + path.toString());
		Files.write( Paths.get(filename), s.getBytes());
	}

}
