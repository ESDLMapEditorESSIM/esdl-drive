package nl.tno.esdl.esdldrive.util;

import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

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

}
