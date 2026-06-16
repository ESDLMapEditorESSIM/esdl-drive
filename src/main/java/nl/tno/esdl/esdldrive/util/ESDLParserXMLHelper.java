package nl.tno.esdl.esdldrive.util;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.impl.XMLHelperImpl;


/**
 * Allows to create hooks in the parser, e.g. rename certain classes.
 * For now it counts the classes it creates to generate an indication of the size of the model loaded.
 * @see 
 */
public class ESDLParserXMLHelper extends XMLHelperImpl {
		
		private int objectCount = 0;
		
		@Override
		public EObject createObject(EFactory eFactory, EClassifier type) {
			this.objectCount++;
			return super.createObject(eFactory, type);
		}
		
		/**
		 * 
		 * @return sum of elements in the XML
		 */
		public int getElementCount() {
			return objectCount;
		}
		
	}