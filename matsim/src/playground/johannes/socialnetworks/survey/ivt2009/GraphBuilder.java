/* *********************************************************************** *
 * project: org.matsim.*
 * GraphBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.johannes.socialnetworks.survey.ivt2009;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;

import playground.johannes.socialnetworks.graph.spatial.SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialVertex;
import playground.johannes.socialnetworks.graph.spatial.io.KMLEgoNetWriter;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialPajekWriter;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class GraphBuilder {
	
	private Logger logger = Logger.getLogger(GraphBuilder.class);
	
	private CoordinateTransformation transform = new WGS84toCH1903LV03();
	
	private KeyGenerator keyGenerator;
	
	private BasicPopulation population;
	
	private SampledSocialNetFactory<BasicPerson<?>> factory;
	
	private SampledSocialNet<BasicPerson<?>> socialnet;
	
	private Map<Id, SampledEgo<BasicPerson<?>>> idVertexMapping;
	
	private Map<String, SampledEgo<BasicPerson<?>>> userIdVertexMapping;

	public SampledSocialNet<BasicPerson<?>> buildSocialNet(String userDataFile, String snowballDataFile) {
		try {
			CSVReader csvReader = new CSVReader();
			Map<String, String[]> userData = csvReader.readUserData(userDataFile);
			Map<String, Map<String, String>> snowballData = csvReader.readSnowballData(snowballDataFile);
			
			population = new PopulationImpl();
			keyGenerator = new KeyGenerator();
			factory = new SampledSocialNetFactory<BasicPerson<?>>();
			socialnet = factory.createGraph();
			idVertexMapping = new HashMap<Id, SampledEgo<BasicPerson<?>>>();
			userIdVertexMapping = new HashMap<String, SampledEgo<BasicPerson<?>>>();
			
			buildEgoPersons(userData, snowballData);
			buildAlters(snowballData);
			
			return socialnet;
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void buildEgoPersons(Map<String, String[]> userData,
			Map<String, Map<String, String>> snowballData) {
		logger.info("Building egos...");
			
		for (String userId : userData.keySet()) {
			String[] userAttributes = userData.get(userId);
			Map<String, String> egoData = snowballData.get(userId);
			if (egoData != null) {
				/*
				 * required: snowball iteration
				 */
				String[] tokens = userAttributes[2].split(" ");
				if (tokens.length == 2) {
					int num = Integer.parseInt(tokens[1]);
					int iteration = -1;
					if (num < 1000)
						iteration = 0;
					else if (num < 10000)
						iteration = 1;
					else if (num < 100000)
						iteration = 2;

					System.out.println("Num="+num+"; it="+iteration);
					/*
					 * required! last known home location
					 */
					Coord homeLoc = egoHomeLocation(userId, egoData);
					if (homeLoc != null) {
						/*
						 * person id is surname + name
						 */
						Id id = createPersonId(userAttributes[0], userAttributes[1]);
						/*
						 * create a person and add a home activity
						 */
						BasicPerson<?> person = createPerson(id, homeLoc);
						/*
						 * add other person attributes...
						 */
						population.addPerson(person);
						SampledEgo<BasicPerson<?>> ego = factory.addVertex(socialnet, person, iteration);
						ego.sample(iteration);
						idVertexMapping.put(person.getId(), ego);
						userIdVertexMapping.put(userId, ego);
						
					} else {
						logger.warn(String.format(
											"Missing home location for user %1$s. Dropping user!",
											userId));
					}
				} else {
					logger.warn("Cannot determin snwoball iteration. Droppging user!");
				}
			} else {
				logger.warn(String.format(
						"No snowball data found for user %1$s!", userId));
			}
		}
		
		logger.info(String.format("Built %1$s egos, dropped %2$s egos.", socialnet.getVertices().size(), userData.size()));
	}
	
	private Coord egoHomeLocation(String userId, Map<String, String> egoData) {
		SortedMap<Integer, String> homeLocs = new TreeMap<Integer, String>();
		for(int i = KeyGenerator.NUM_HOMELOCS; i > 0; i--) {
			String year = egoData.get(keyGenerator.egoHomeLocationYearKey(i));
			if(year != null) {
				String homeLoc = egoData.get(keyGenerator.egoHomeLocationCoordKey(i));
				if(homeLoc != null)
					homeLocs.put(new Integer(year), homeLoc);
				else
					logger.warn(String.format("Missing home location string. userId = %1$s, entry = %2$s", userId, i));
			}
		}
		
		if(homeLocs.isEmpty())
			return null;
		else
			return decodeCoordinate(homeLocs.get(homeLocs.lastKey()));
	}
	
	private void buildAlters(Map<String, Map<String, String>> snowballData) {
		logger.info("Building alters...");
		int valid = 0;
		int invalid = 0;
		
		for (String userId : snowballData.keySet()) {
			SampledEgo<BasicPerson<?>> ego = userIdVertexMapping.get(userId);
			if (ego != null) {
				Map<String, String> egoData = snowballData.get(userId);

				for (int i = KeyGenerator.NUM_ALTERS_1; i > 0; i--) {
					/*
					 * get the alter's name
					 */
					String name = egoData.get(keyGenerator.alter1Key(i));
					if (name != null && name.length() > 0) {
						name = cleanName(name);
						if (buildAlter(KeyGenerator.ALTER_1_KEY, name, userId,
								ego, i, egoData))
							valid++;
						else
							invalid++;
					}
				}

				for (int i = KeyGenerator.NUM_ALTERS_2; i > 0; i--) {
					/*
					 * get the alter's name
					 */
					String name = egoData.get(keyGenerator.alter2Key(i));
					if (name != null && name.length() > 0) {
						name = cleanName(name);
						if (buildAlter(KeyGenerator.ALTER_2_KEY, name, userId,
								ego, i, egoData))
							valid++;
						else
							invalid++;
					}
				}
			}
		}

		logger.info(String.format("Built %1$s alter, dropped %2$s.", valid,
				invalid));
	}
	
	private boolean buildAlter(String alterKey, String name, String userId, SampledEgo<BasicPerson<?>> ego, int counter, Map<String, String> egoData) {
		Id alterId = new IdImpl(name);
		/*
		 * check if we already sampled this vertex
		 */
		SampledEgo<BasicPerson<?>> alter = idVertexMapping.get(alterId);
		
		if (alter == null) {
			/*
			 * get the coordinate string
			 */
			String coordStr = egoData.get(keyGenerator.alterLocationCoordKey(alterKey, counter));
			
			if(coordStr != null) {
				/*
				 * decode the coordinate string
				 */
				Coord homeLoc = decodeCoordinate(coordStr);
				
				if(homeLoc != null) {
					/*
					 * create a person and a vertex
					 */
					BasicPerson<?> person = createPerson(alterId, homeLoc);
					population.addPerson(person);
					alter = factory.addVertex(socialnet, person, ego.getIterationSampled());
					idVertexMapping.put(person.getId(), alter);
					userIdVertexMapping.put(userId, alter);
					alter.detect(ego.getIterationSampled());
					alter.setRecruitedBy(ego);
				}
			} else {
				logger.warn(String.format("Missing home location for alter %1$s of user %2$s. Dropping alter!", counter, userId));
			}
		}
		
		if(alter != null) {
			factory.addEdge(socialnet, ego, alter);
			alter.detect(Math.min(ego.getIterationSampled(), alter.getIterationDetected()));
			return true;
		} else
			return false;
	
	}
	
	private Id createPersonId(String surname, String name) {
		StringBuilder builder = new StringBuilder(surname.length() + name.length());
		builder.append(surname);
		builder.append(" ");
		/*
		 * reduce Doppelnamen
		 */
		name = cleanName(name);
		
		builder.append(name);
		return new IdImpl(builder.toString());
	}
	
	private String cleanName(String name) {
//		int idx = name.indexOf("-"); 
//		if(idx > -1) {
//			name = name.substring(0, idx);
//		}
//		TODO: Need to think about that...
		return name;
	}
	
	private BasicPerson<?> createPerson(Id id, Coord coord) {
		BasicPerson<BasicPlan<?>> person = population.getFactory().createPerson(id);
		BasicPlan<?> plan = population.getFactory().createPlan(person);
		BasicActivity activity = population.getFactory().createActivityFromCoord("home", coord);
		plan.addActivity(activity);
		person.addPlan(plan);

		return person;
	}
	
	private Coord decodeCoordinate(String coordString) {
		String[] tokens = coordString.split("@");
		if(tokens.length >= 2) {
			String latitude = tokens[0];
			String longitude = tokens[1];
//			return new CoordImpl(Double.parseDouble(longitude), Double.parseDouble(latitude));
			return transform.transform(new CoordImpl(Double.parseDouble(longitude), Double.parseDouble(latitude)));
		} else {
			logger.warn("Invalid coordinate string!");
			return null;
		}
	}
	
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		GraphBuilder builder = new GraphBuilder();
		SampledSocialNet<BasicPerson<?>> socialnet = builder.buildSocialNet(args[0], args[1]);
		System.out.println(SampledGraphStatistics.degreeDistribution(socialnet).mean());
		System.out.println(SampledGraphStatistics.localClusteringDistribution(socialnet).mean());
		
		Distribution.writeHistogram(SampledGraphStatistics.degreeDistribution(socialnet).absoluteDistribution(), "/Users/fearonni/Desktop/degree.hist.txt");
		Distribution.writeHistogram(SampledGraphStatistics.edgeLenghtDistribution(socialnet).absoluteDistribution(500), "/Users/fearonni/Desktop/edgelength.hist.txt");
//		
		Population2SpatialGraph pop2graph = new Population2SpatialGraph();
		SpatialGraph g2 = pop2graph.read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/zrh100km/plans/plans.10.xml");
		Set<Coord> coords = new HashSet<Coord>();
		for(SpatialVertex v : g2.getVertices()) {
			coords.add(v.getCoordinate());
		}
		Distribution edgelength = SampledGraphStatistics.normalizedEdgeLengthDistribution(socialnet, g2, 1000);
		Distribution.writeHistogram(edgelength.absoluteDistribution(500), "/Users/fearonni/Desktop/edgelength.norm.hist.txt");
//		
		KMLEgoNetWriter writer = new KMLEgoNetWriter();
		writer.setCoordinateTransformation(new CH1903LV03toWGS84());
		writer.setDrawEdges(true);
		writer.setVertexStyle(new KMLSnowballVertexStyle(writer.getVertexIconLink()));
		writer.setVertexDescriptor(new KMLSnowballDescriptor());
		writer.write(socialnet, (Set) SnowballPartitions.createSampledPartition(socialnet, 0), 10, "/Users/fearonni/vsp-work/work/socialnets/data/ivt2009-pretest/egonets.kmz");
		
		SpatialPajekWriter pwriter = new SpatialPajekWriter();
		pwriter.write(socialnet, "/Users/fearonni/Desktop/egonet.net");
	}
}
