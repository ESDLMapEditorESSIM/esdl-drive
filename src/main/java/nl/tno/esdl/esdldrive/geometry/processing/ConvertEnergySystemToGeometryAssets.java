package nl.tno.esdl.esdldrive.geometry.processing;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;

import esdl.EnergySystem;
import esdl.SubPolygon;
import nl.tno.esdl.esdldrive.geometry.entities.ESDLGeoObjects;

public class ConvertEnergySystemToGeometryAssets {
	
	/**
	 *  WGS84/EPSG:4326 (Lat/Lng)
	 *  EPSG:3857(Web Mercator)
	 */
	public static int SRID = 4326;  // WGS84 
	
	private static final GeometryFactory WGS84_GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), SRID);
	private static final WKTReader WKT_READER = new WKTReader(WGS84_GEOMETRY_FACTORY);
	private static final WKBReader WKB_READER = new WKBReader(WGS84_GEOMETRY_FACTORY);
	
	public static List<ESDLGeoObjects> convert(EnergySystem energySystem) {
		String es_id = energySystem.getId();
		if (es_id == null || es_id.isBlank()) {
			throw new IllegalArgumentException("Input Energy system has no ID");
		}
		ArrayList<ESDLGeoObjects> objects = new ArrayList<ESDLGeoObjects>();
		
		if (energySystem instanceof CDOObject) {
			// fetch as much as possible without a lot of round trips to the cdo-server
			energySystem.cdoPrefetch(CDORevision.DEPTH_INFINITE );
		}
		
		final TreeIterator<EObject> iter = energySystem.eAllContents();
		while (iter.hasNext()) {
			final EObject obj = iter.next();
			if (obj instanceof esdl.Geometry /*&& obj.eContainer() != null && !(obj.eContainer() instanceof esdl.Geometry) */ ) {
				esdl.Geometry esdlGeometry = (esdl.Geometry) obj;
				System.out.println("Handling " + obj.eContainer());
				Geometry geometry = createJSTGeometry(esdlGeometry);
				if (geometry == null) {
					continue; // skip this assets as it has an invalid/wrong geometry
				}
				EObject container = obj.eContainer();
				if (container instanceof esdl.Asset) {
					esdl.Asset asset = (esdl.Asset) container;
					ESDLGeoObjects geo_asset = new ESDLGeoObjects(es_id, asset.getId(), asset.getName(), asset.eClass().getName(), capability(asset), geometry );
					objects.add(geo_asset);
					iter.prune();  // skip sub geometries ( e.g. points of a line) as we handled this one
				}
				if (container instanceof esdl.Area) {
					esdl.Area area = (esdl.Area) container;
					ESDLGeoObjects geo_asset = new ESDLGeoObjects(es_id, area.getId(), area.getName(), area.eClass().getName(), null, geometry );
					objects.add(geo_asset);
					iter.prune();  // skip sub geometries ( e.g. points of a line) as we handled this one
				}
				//area
				
				//potential
				
				//note.mapLocation
			}
		}
		return objects;
	}

	
	public static String capability(esdl.Asset asset) {
		if (asset instanceof esdl.Producer) {
			return esdl.Producer.class.getSimpleName();
		} else if (asset instanceof esdl.Consumer) {
			return esdl.Consumer.class.getSimpleName();
		} else if (asset instanceof esdl.Conversion) {
			return esdl.Conversion.class.getSimpleName();
		} else if (asset instanceof esdl.Storage) {
			return esdl.Storage.class.getSimpleName();
		} else if (asset instanceof esdl.Transport) {
			return esdl.Transport.class.getSimpleName();
		} else {
			return null;
		}
	}
	
	/**
	 * 
	 * @param esdlGeometry
	 * @return JST Geometry or null if not in the correct CRS/SRID (e.g. Simple CRS for assets in buildings/compound assets)
	 */
	private static Geometry createJSTGeometry(esdl.Geometry esdlGeometry) {
		String crs = esdlGeometry.getCRS();
		if (crs == null || crs.equalsIgnoreCase("WGS84") || crs.equalsIgnoreCase("4326") || crs.equals("")) { 
			if (esdlGeometry instanceof esdl.Point) {
				esdl.Point esdlPoint = (esdl.Point) esdlGeometry;
				Coordinate coord = new Coordinate(esdlPoint.getLat(), esdlPoint.getLon());
				return WGS84_GEOMETRY_FACTORY.createPoint(coord);
			} else if (esdlGeometry instanceof esdl.Line) {
				esdl.Line line = (esdl.Line) esdlGeometry;
				return createLineString(line);
			} else if (esdlGeometry instanceof esdl.Polygon) {
				esdl.Polygon polygon = (esdl.Polygon) esdlGeometry;
				return createPolygon(polygon);
			} else if (esdlGeometry instanceof esdl.MultiLine) {
				esdl.MultiLine multiLine = (esdl.MultiLine) esdlGeometry;
				LineString lines[] = new LineString[multiLine.getLine().size()];
				for (int i=0;i<multiLine.getLine().size(); i++) {
					lines[i] = createLineString(multiLine.getLine().get(i));
				}
				return WGS84_GEOMETRY_FACTORY.createMultiLineString(lines);
			} else if (esdlGeometry instanceof esdl.MultiPolygon) {
				esdl.MultiPolygon multiPolygon = (esdl.MultiPolygon) esdlGeometry;
				Polygon polygons[] = new Polygon[multiPolygon.getPolygon().size()];
				for (int i=0;i<multiPolygon.getPolygon().size(); i++) {
					polygons[i] = createPolygon(multiPolygon.getPolygon().get(i));
				}
				return WGS84_GEOMETRY_FACTORY.createMultiPolygon(polygons);
			} else if (esdlGeometry instanceof esdl.WKT) {
				esdl.WKT wkt = (esdl.WKT) esdlGeometry;
				try {
					return WKT_READER.read(wkt.getValue());
				} catch (ParseException e) {
					e.printStackTrace();
					return null;
				}
			} else if (esdlGeometry instanceof esdl.WKB) {
				esdl.WKB wkb = (esdl.WKB) esdlGeometry;
				try {
					return WKB_READER.read(wkb.getValue().getBytes(StandardCharsets.UTF_8));
				} catch (ParseException e) {
					e.printStackTrace();
					return null;
				}
			}
		} else if (crs.equalsIgnoreCase("simple")) {
			// ESDL MapEditor uses 'Simple' (x,y) coordinates for assets in buildings or compound assets
			// return null to indicate this
			return null;
		}
		return null;
	}


	private static Polygon createPolygon(esdl.Polygon polygon) {
		SubPolygon exterior = polygon.getExterior();
		Coordinate[] ext_points = exterior.getPoint().stream().map(p -> new Coordinate(p.getLat(), p.getLon(), p.getElevation())).toArray(Coordinate[]::new);
		LinearRing shell = WGS84_GEOMETRY_FACTORY.createLinearRing(ext_points);
		LinearRing[] holes = new LinearRing[polygon.getInterior().size()];
		for (int i = 0; i< polygon.getInterior().size(); i++) {
			SubPolygon interior = polygon.getInterior().get(i);
			Coordinate[] int_coords = interior.getPoint().stream().map( p -> new Coordinate(p.getLat(), p.getLon(), p.getElevation()) ).toArray(Coordinate[]::new);
			holes[i] = WGS84_GEOMETRY_FACTORY.createLinearRing(int_coords);
		}
		return WGS84_GEOMETRY_FACTORY.createPolygon(shell, holes);
	}
	
	private static LineString createLineString(esdl.Line line) {
		Coordinate[] coords = line.getPoint().stream().map(p -> new Coordinate(p.getLat(), p.getLon(), p.getElevation())).toArray(Coordinate[]::new);
		return WGS84_GEOMETRY_FACTORY.createLineString(coords);
		
	}

}
