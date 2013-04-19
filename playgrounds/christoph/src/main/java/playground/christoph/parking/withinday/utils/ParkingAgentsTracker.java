/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingAgentsTracker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.parking.withinday.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure;

public class ParkingAgentsTracker implements LinkEnterEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler,
		ActivityStartEventHandler, ActivityEndEventHandler, MobsimInitializedListener, MobsimAfterSimStepListener {

	private static final Logger log = Logger.getLogger(ParkingAgentsTracker.class);
	
	// TODO: allow each agent to decided whether starting its parking search or not
	
	protected final Scenario scenario;
	protected final ParkingInfrastructure parkingInfrastructure;
	private final double distance;

	private final Set<Id> carLegAgents;
	private final Set<Id> searchingAgents;
	private final Set<Id> linkEnteredAgents;
	private final Set<Id> lastTimeStepsLinkEnteredAgents;
	private final Map<Id, ActivityFacility> nextActivityFacilityMap;
	protected final Map<Id, PlanBasedWithinDayAgent> agents;
	private final Map<Id, Id> selectedParkingsMap;
	private final Set<Id> recentlyArrivedDrivers;
	private final Map<Id, Id> recentlyDepartingDrivers;
	
	/**
	 * Tracks agents' car legs and check whether they have to start their parking search.
	 * 
	 * @param scenario
	 * @param distance
	 *            defines in which distance to the destination of a car trip an
	 *            agent starts its parking search
	 */
	public ParkingAgentsTracker(Scenario scenario, ParkingInfrastructure parkingInfrastructure, double distance) {
		this.scenario = scenario;
		this.parkingInfrastructure = parkingInfrastructure;
		this.distance = distance;

		this.carLegAgents = new HashSet<Id>();
		this.linkEnteredAgents = new HashSet<Id>();
		this.selectedParkingsMap = new HashMap<Id, Id>();
		this.lastTimeStepsLinkEnteredAgents = new TreeSet<Id>(); // This set has to be deterministic!
		this.searchingAgents = new HashSet<Id>();
		this.nextActivityFacilityMap = new HashMap<Id, ActivityFacility>();
		this.recentlyArrivedDrivers = new HashSet<Id>();
		this.recentlyDepartingDrivers = new HashMap<Id, Id>();
		this.agents = new HashMap<Id, PlanBasedWithinDayAgent>();
	}

	public Set<Id> getSearchingAgents() {
		return Collections.unmodifiableSet(this.searchingAgents);
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), (ExperimentalBasicWithindayAgent) agent);
		}
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		lastTimeStepsLinkEnteredAgents.clear();
		lastTimeStepsLinkEnteredAgents.addAll(linkEnteredAgents);
		linkEnteredAgents.clear();
	}

	/*
	 * Agents that are searching and that have just entered a new link.
	 */
	public Set<Id> getLinkEnteredAgents() {
		return lastTimeStepsLinkEnteredAgents;
	}

	public void setSelectedParking(Id agentId, Id parkingFacilityId) {
		
//		log.info("Select parking facility " + parkingFacilityId + " for agent " + agentId);
		
		selectedParkingsMap.put(agentId, parkingFacilityId);
		
//		Id vehicleId = this.parkingInfrastructure.getVehicleId(agents.get(agentId).getSelectedPlan().getPerson());
		Id vehicleId = agentId;	// so far, this is true...
		this.parkingInfrastructure.reserveParking(vehicleId, parkingFacilityId);
	}

	public Id getSelectedParking(Id agentId) {
		return selectedParkingsMap.get(agentId);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		
		// get the facility Id where the agent performed the activity
		Id facilityId = this.recentlyDepartingDrivers.remove(event.getPersonId());

		if (event.getLegMode().equals(TransportMode.car)) {
			this.carLegAgents.add(event.getPersonId());

			// Get the agent's next non-parking activity and the facility where it is performed.
			Activity nextNonParkingActivity = getNextNonParkingActivity(event.getPersonId());
			ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(nextNonParkingActivity.getFacilityId());
			this.nextActivityFacilityMap.put(event.getPersonId(), facility);
			
			// Get the coordinate of the next non-parking activity's facility.
			Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
			double distanceToNextActivity = CoordUtils.calcDistance(facility.getCoord(), coord);

			/*
			 * If the agent is within distance 'd' to target activity or OR if the
			 * agent enters the link where its next non-parking activity is
			 * performed, mark him as searching agent.
			 * 
			 * (this is actually handling a special case, where already at departure time
			 * the agent is within distance 'd' of next activity).
			 */
			if (shouldStartSearchParking(event.getLinkId(), facility.getLinkId(), distanceToNextActivity)) {
				searchingAgents.add(event.getPersonId());
			}
			
			// mark the parking slot as free
			Id vehicleId = event.getPersonId();	// so far, this is true...
			this.parkingInfrastructure.unParkVehicle(vehicleId, facilityId);
			
			/*
			 * TODO: Check whether an agent's leg starts and ends at the same link.
			 * This should be prevented by the replanning framework.
			 */
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		this.recentlyDepartingDrivers.put(event.getPersonId(), event.getFacilityId());
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.searchingAgents.remove(event.getPersonId());
		this.linkEnteredAgents.remove(event.getPersonId());
		this.nextActivityFacilityMap.remove(event.getPersonId());
		this.selectedParkingsMap.remove(event.getPersonId());
		
		boolean wasCarTrip = this.carLegAgents.remove(event.getPersonId());
		if (wasCarTrip) this.recentlyArrivedDrivers.add(event.getPersonId());

	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		
		boolean wasCarTrip = this.recentlyArrivedDrivers.remove(event.getPersonId());
		if (wasCarTrip) {
			Id vehicleId = event.getPersonId(); // so far, this is true...
			this.parkingInfrastructure.parkVehicle(vehicleId, event.getFacilityId());
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (carLegAgents.contains(event.getPersonId())) {
			if (!searchingAgents.contains(event.getPersonId())) {
				Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
				ActivityFacility facility = nextActivityFacilityMap.get(event.getPersonId());
				double distanceToNextActivity = CoordUtils.calcDistance(facility.getCoord(), coord);
				
				if (shouldStartSearchParking(event.getLinkId(), facility.getLinkId(), distanceToNextActivity)) {
					searchingAgents.add(event.getPersonId());
					linkEnteredAgents.add(event.getPersonId());
				}
			}
			// the agent is already searching: update its position
			else {
				linkEnteredAgents.add(event.getPersonId());
			}
		}
	}
	
	/*
	 * The currentPlanElement is a car leg, which is followed by a
	 * parking activity and a walking leg to the next non-parking
	 * activity.
	 */
	private Activity getNextNonParkingActivity(Id agentId) {

		PlanBasedWithinDayAgent agent = this.agents.get(agentId);
		Plan executedPlan = agent.getSelectedPlan();
		int planElementIndex = agent.getCurrentPlanElementIndex();
		
		Activity nextNonParkingActivity = (Activity) executedPlan.getPlanElements().get(planElementIndex + 3);		
		return nextNonParkingActivity;
	}
	
	/*
	 * If the agent is within the parking radius or if the agent enters the link 
	 * where its next non-parking activity is performed.
	 */
	private boolean shouldStartSearchParking(Id currentLinkId, Id nextActivityLinkId, double distanceToNextActivity) {
		return distanceToNextActivity <= distance || nextActivityLinkId.equals(currentLinkId);
	}

	@Override
	public void reset(int iteration) {
		this.agents.clear();
		this.carLegAgents.clear();
		this.searchingAgents.clear();
		this.linkEnteredAgents.clear();
		this.selectedParkingsMap.clear();
		this.nextActivityFacilityMap.clear();
		this.lastTimeStepsLinkEnteredAgents.clear();
		this.recentlyArrivedDrivers.clear();
		this.recentlyDepartingDrivers.clear();		
	}

}