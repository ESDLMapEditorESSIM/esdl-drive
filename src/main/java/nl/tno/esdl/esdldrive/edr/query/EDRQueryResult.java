package nl.tno.esdl.esdldrive.edr.query;

public class EDRQueryResult {
	
	private final String name;
	private final String path;
	private final String type;
	/**
	 * @param name
	 * @param path
	 * @param type
	 */
	public EDRQueryResult(String name, String path, String type) {
		super();
		this.name = name;
		this.path = path;
		this.type = type;
	}
	
	public String getName() {
		return name;
	}
	public String getPath() {
		return path;
	}
	public String getType() {
		return type;
	}
}
