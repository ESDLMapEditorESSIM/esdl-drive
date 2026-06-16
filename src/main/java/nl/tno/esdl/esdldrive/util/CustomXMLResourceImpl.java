package nl.tno.esdl.esdldrive.util;

import org.eclipse.emf.ecore.xmi.XMLHelper;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;


/***
 * Custom XML Resource implementation that allows to hook into the parser using a dedicated 
 * XML Helper
 * @see ESDLParserXMLHelper
 */
public class CustomXMLResourceImpl extends XMLResourceImpl {
	ESDLParserXMLHelper esdlParserXMLHelper = new ESDLParserXMLHelper();
	
	public CustomXMLResourceImpl() {
		super();
	}
	
	/**
	 * 
	 * @return the XML element count when parsing the full XML
	 */
	public int getElementCount() {
		return esdlParserXMLHelper.getElementCount();
	}

	
	/**
	 * create a custom XML Helper
	 */
	@Override
	protected XMLHelper createXMLHelper() {
		return esdlParserXMLHelper;
	}
	
	
}
