package nl.tno.esdl.esdldrive.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.cdo.common.model.EMFUtil;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy.Props;
import org.eclipse.emf.cdo.server.internal.db.DBAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.net4j.db.postgresql.PostgreSQLAdapter;
import org.eclipse.net4j.spi.db.DBAdapter;
import org.eclipse.net4j.util.ImplementationError;
import org.eclipse.net4j.util.StringUtil;

public class CDOQueryUtil {
	protected static final String NAME_SEPARATOR = "_"; //$NON-NLS-1$

	protected static final String TYPE_PREFIX_FEATURE = "F"; //$NON-NLS-1$

	protected static final String TYPE_PREFIX_CLASS = "C"; //$NON-NLS-1$

	protected static final String TYPE_PREFIX_PACKAGE = "P"; //$NON-NLS-1$

	protected static final String GENERAL_PREFIX = "X"; //$NON-NLS-1$

	protected static final String GENERAL_SUFFIX = "0"; //$NON-NLS-1$

	/**
	 * Prefix for unsettable feature helper columns
	 */
	protected static final String CDO_SET_PREFIX = "cdo_set_"; //$NON-NLS-1$

	protected static final String FEATURE_TABLE_SUFFIX = "_list"; //$NON-NLS-1$

	private Map<String, String> properties;

	private DBAdapter dbAdapter;

	
	/**
	 * Creates new CDOQueryUtil class with POstgreSQLAdapter as basis
	 */
	public CDOQueryUtil() {
		dbAdapter = new PostgreSQLAdapter();
		getProperties().put(Props.TABLE_NAME_PREFIX, "esdl");
	}
	
	public CDOQueryUtil(DBAdapter dbAdapter) {
		this.dbAdapter = dbAdapter;
	}

	private DBAdapter getDBAdapter() {
		return dbAdapter;
	}

	public synchronized Map<String, String> getProperties() {
		if (properties == null) {
			properties = new HashMap<>();
		}

		return properties;
	}

	public int getMaxTableNameLength() {
		String value = getProperties().get(Props.MAX_TABLE_NAME_LENGTH);
		return value == null ? getDBAdapter().getMaxTableNameLength() : Integer.valueOf(value);
	}

	public int getMaxFieldNameLength() {
		String value = getProperties().get(Props.MAX_FIELD_NAME_LENGTH);
		return value == null ? getDBAdapter().getMaxFieldNameLength() : Integer.valueOf(value);
	}

	public boolean isQualifiedNames() {
		String value = getProperties().get(Props.QUALIFIED_NAMES);
		return value == null ? false : Boolean.valueOf(value);
	}

	public boolean isForceNamesWithID() {
		String value = getProperties().get(Props.FORCE_NAMES_WITH_ID);
		return value == null ? false : Boolean.valueOf(value);
	}

	public String getTableNamePrefix() {
		String value = getProperties().get(Props.TABLE_NAME_PREFIX);
		return StringUtil.safe(value);
	}
	
	 private String getTableNamePrefix(EModelElement element)
	  {
	    String prefix = StringUtil.safe(DBAnnotation.TABLE_NAME_PREFIX.getValue(element));
	    if (prefix.length() != 0 && !prefix.endsWith(NAME_SEPARATOR))
	    {
	      prefix += NAME_SEPARATOR;
	    }

	    EObject eContainer = element.eContainer();
	    if (eContainer instanceof EModelElement)
	    {
	      EModelElement parent = (EModelElement)eContainer;
	      prefix = getTableNamePrefix(parent) + prefix;
	    }

	    return prefix;
	  }

	public String getTableName(EClass eClass, EStructuralFeature feature) {
		String name = DBAnnotation.TABLE_NAME.getValue(eClass);
		if (name == null) {
			name = isQualifiedNames() ? EMFUtil.getQualifiedName(eClass, NAME_SEPARATOR) : eClass.getName();
		}

		name += NAME_SEPARATOR;
		name += feature.getName();
		name += FEATURE_TABLE_SUFFIX;

		String prefix = getTableNamePrefix();
		if (prefix.length() != 0 && !prefix.endsWith(NAME_SEPARATOR)) {
			prefix += NAME_SEPARATOR;
		}

		prefix += getTableNamePrefix(feature);

		String suffix = TYPE_PREFIX_FEATURE  /* + getUniqueID(feature) */;
		int maxTableNameLength = getMaxTableNameLength();

		return getName(prefix + name, suffix, maxTableNameLength);
	}
	
	 public String getTableName(ENamedElement element)
	  {
	    String name = null;
	    String typePrefix = null;

	    if (element instanceof EClass)
	    {
	      typePrefix = TYPE_PREFIX_CLASS;
	      name = DBAnnotation.TABLE_NAME.getValue(element);
	      if (name == null)
	      {
	        name = isQualifiedNames() ? EMFUtil.getQualifiedName((EClass)element, NAME_SEPARATOR) : element.getName();
	      }
	    }
	    else if (element instanceof EPackage)
	    {
	      typePrefix = TYPE_PREFIX_PACKAGE;
	      name = DBAnnotation.TABLE_NAME.getValue(element);
	      if (name == null)
	      {
	        name = isQualifiedNames() ? EMFUtil.getQualifiedName((EPackage)element, NAME_SEPARATOR) : element.getName();
	      }
	    }
	    else
	    {
	      throw new ImplementationError("Unknown element: " + element); //$NON-NLS-1$
	    }

	    String prefix = getTableNamePrefix();
	    if (prefix.length() != 0 && !prefix.endsWith(NAME_SEPARATOR))
	    {
	      prefix += NAME_SEPARATOR;
	    }

	    prefix += getTableNamePrefix(element);

	    String suffix = typePrefix /* + getUniqueID(element) */;
	    int maxTableNameLength = getMaxTableNameLength();

	    return getName(prefix + name, suffix, maxTableNameLength);
	  }


	private String getName(String name, String suffix, int maxLength) {
		if (!getDBAdapter().isValidFirstChar(name.charAt(0))) {
			name = GENERAL_PREFIX + name;
		}

		boolean forceNamesWithID = isForceNamesWithID();
		if (!forceNamesWithID && getDBAdapter().isReservedWord(name)) {
			name = name + GENERAL_SUFFIX;
		}

		if (name.length() > maxLength || forceNamesWithID) {
			suffix = NAME_SEPARATOR + suffix.replace('-', 'S');
			int length = Math.min(name.length(), maxLength - suffix.length());
			if (length < 0) {
				// Most likely CDOIDs are client side-assigned, i.e., meta IDs are extrefs. See
				// getUniqueID()
				throw new IllegalStateException("Suffix is too long: " + suffix);
			}

			name = name.substring(0, length) + suffix;
		}

		return name;
	}

}
