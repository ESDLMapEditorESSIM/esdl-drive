package nl.tno.esdl.esdldrive;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import org.junit.Test;

public class MicsTests {

	@Test
	public void test() throws URISyntaxException, UnsupportedEncodingException {
		String s = "http://www/path/file name.txt";
		//String u = URLEncoder.encode(s, "UTF-8");
		org.eclipse.emf.common.util.URI ecore_u = org.eclipse.emf.common.util.URI.createURI(s);
		System.out.println(ecore_u);
		System.out.println(org.eclipse.emf.common.util.URI.encodeFragment(s, true));

		
		
		
		String u = s.replace(" ", "%20");
		URI uri = new URI(u);
		System.out.println(uri);
		System.out.println(URLDecoder.decode(uri.toString(), "UTF-8"));
		
		String decoded = org.eclipse.emf.common.util.URI.decode(uri.toString());
		System.out.println(decoded);
	}

}
