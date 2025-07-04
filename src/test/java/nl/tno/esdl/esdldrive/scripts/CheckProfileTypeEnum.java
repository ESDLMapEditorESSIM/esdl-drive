package nl.tno.esdl.esdldrive.scripts;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.view.CDOQuery;
import org.eclipse.emf.cdo.view.CDOView;

import esdl.EsdlPackage;
import esdl.InfluxDBProfile;
import nl.tno.esdl.esdldrive.util.CDOQueryUtil;

public class CheckProfileTypeEnum {
	public CheckProfileTypeEnum() throws IOException {
		CDOConnection conn = new CDOConnection();
		CDOSession session = conn.setupCDOSession();

		CDOView view = session.openView();
		CDOQueryUtil cdoQueryUtil = new CDOQueryUtil();
		String tableName = cdoQueryUtil.getTableName(EsdlPackage.eINSTANCE.getInfluxDBProfile());
		String fieldName = EsdlPackage.eINSTANCE.getGenericProfile_ProfileType().getName().toLowerCase();
		CDOQuery query = view.createQuery("sql", "SELECT * FROM " + tableName + " WHERE " + fieldName + " <> 0");
		List<InfluxDBProfile> result = query.getResult(InfluxDBProfile.class);
		
		

		TreeSet<String> h = new TreeSet<String>();
		TreeSet<LocalDateTime> d = new TreeSet<LocalDateTime>();
		TreeMap<String, LocalDateTime> e = new TreeMap<String, LocalDateTime>();

		for (InfluxDBProfile influxDBProfile : result) {
			if (influxDBProfile == null) {
				System.err.println("Result is null");
			} else {
				CDOResource cdoResource = influxDBProfile.cdoResource();
				String path = cdoResource.getPath();
				h.add(path);
				long timeStamp = influxDBProfile.cdoRevision(true).getTimeStamp();
				Instant instant = Instant.ofEpochMilli(timeStamp);
				LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
				boolean hasQaU = influxDBProfile.getProfileQuantityAndUnit() != null;
				path = hasQaU? path + "; QaU" : path; 
				d.add(dateTime);
				if (e.containsKey(path)) {
					if (e.get(path).isBefore(dateTime) ) {
						e.put(path, dateTime);
					}
				} else {
					e.put(path, dateTime);
				}
				System.out.println(path + ": " + " date:" + dateTime + " ID: "
						+ influxDBProfile.cdoID() + ", profileType:" + influxDBProfile.getProfileType().getName());

			}
		}

		System.out.println();
		System.out.println(h.size());
		h.stream().forEach(s -> {
			System.out.println("-" + s);
		});

		System.out.println();
		System.out.println(d.size());
		d.stream().forEach(s -> {
			System.out.println("-" + s);
		});

		System.out.println();
		System.out.println(e.size());
		e.forEach((k,v) -> {
			System.out.println("-" + k + ": " + v);
		});

	
		
		conn.shutdownSession(session);

	}

	public static void main(String[] args) throws IOException {
		new CheckProfileTypeEnum();

	}

}
