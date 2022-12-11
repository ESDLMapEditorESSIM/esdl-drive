package nl.tno.esdl.esdldrive.util;

import java.text.StringCharacterIterator;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;

public class ESDLUtil {

	public static String eObjectToString(EObject es, boolean references) {
		StringBuffer sb = new StringBuffer();
		EList<EAttribute> eAllAttributes = es.eClass().getEAttributes();
		sb.append(es.eClass().getName());
		sb.append("@" + es.hashCode() + "(");
		String attributes = eAllAttributes.stream()
			.filter(a -> es.eIsSet(a))
			.map(a -> a.getName()  + "=" + es.eGet(a))
			.collect(Collectors.joining(", "));
		sb.append(attributes);
			
		if (references) {
			EList<EReference> eAllReferences = es.eClass().getEReferences();
			String refs = "";
			for (EReference r : eAllReferences) {
				if (r.isContainment()) {
					if (r.isMany()) {
						@SuppressWarnings("unchecked")
						EList<EObject> l = (EList<EObject>) es.eGet(r);
						if (l != null) {
							if (l.size() > 3) {
								refs = r.getName() + ".size=" + l.size();
							} else {
								refs += r.getName() + ":=[";
								refs += l.stream().map(i -> eObjectToString(i, false)).collect(Collectors.joining(", "));
								refs += "] ";
							}
						}
					} else {
						EObject e = (EObject) es.eGet(r);
						if (e != null) {
							refs = r.getName() + ":=" + eObjectToString(e, false);
						}
					}
				}
			}
			if (attributes.length() > 0 && refs.length() > 0) sb.append(", ");
			sb.append(refs);
		}
		sb.append(")");
		return sb.toString();
	}
	
	
	/**
	 * Taken from {@link https://programming.guide/java/formatting-byte-size-to-human-readable-format.html}  
	 * @param bytes
	 * @return
	 */
	public static String bytesSizeToString(long bytes) {
	    if (-1000 < bytes && bytes < 1000) {
	        return bytes + " B";
	    }
	    StringCharacterIterator ci = new StringCharacterIterator("kMGTPE");
	    while (bytes <= -999_950 || bytes >= 999_950) {
	        bytes /= 1000;
	        ci.next();
	    }
	    return String.format("%.1f %cB", bytes / 1000.0, ci.current());
	}
	
	
	/**
	 * Finds an id of an EObject contained in rootObject by iterating though its eAllContents and searching for that id.<br>
	 * If the rootObject contains a lot of contents, this approach is quite slow.
	 * 
	 * @param rootObject contents of this object is searched
	 * @param id id to search for
	 * @return the EObject belonging to that id, or null if not found
	 */
	public static EObject findById(EObject rootObject, String id) {
		TreeIterator<EObject> eAllContents = rootObject.eAllContents();
		while (eAllContents.hasNext()) {
			EObject e = eAllContents.next();
			String objectId = EcoreUtil.getID(e);
			if(objectId != null && objectId.equals(id)) {
				return e;
			}
		};
		return null;
	}

}
