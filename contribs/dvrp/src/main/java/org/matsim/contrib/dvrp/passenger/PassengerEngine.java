/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.passenger;

import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

public interface PassengerEngine
		extends MobsimEngine, DepartureHandler, PassengerRequestEventForwarder.PassengerRequestEventListener {
	boolean pickUpPassenger(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver, PassengerRequest request,
			double now);

	void dropOffPassenger(MobsimDriverAgent driver, PassengerRequest request, double now);
}
