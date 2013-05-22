package org.matsim.contrib.freight.vrp;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.End;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Start;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.contrib.freight.vrp.utils.VrpTourBuilder;
import org.matsim.core.basic.v01.IdImpl;

class Matsim2VrpUtils {

	static Collection<ScheduledTour> createTours(Collection<VehicleRoute> vehicleRoutes, Matsim2VrpMap matsim2vrpMap) {
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		for (VehicleRoute route : vehicleRoutes) {
			org.matsim.contrib.freight.carrier.Tour vehicleTour = createTour(matsim2vrpMap, route);
			ScheduledTour scheduledTour = ScheduledTour.newInstance(vehicleTour,matsim2vrpMap.getCarrierVehicle(route.getVehicle()),vehicleTour.getStart().getExpectedActEnd());
			scheduledTours.add(scheduledTour);
		}
		return scheduledTours;
	}

	private static org.matsim.contrib.freight.carrier.Tour createTour(Matsim2VrpMap matsim2vrpMap, VehicleRoute route) {
		TourImpl tour = route.getTour();
		org.matsim.contrib.freight.carrier.Tour.Builder tourBuilder = org.matsim.contrib.freight.carrier.Tour.Builder.newInstance();
		for (TourActivity act : tour.getActivities()) {
			if (act instanceof Pickup) {
				Shipment shipment = (Shipment) ((Pickup) act).getJob();
				CarrierShipment carrierShipment = matsim2vrpMap.getCarrierShipment(shipment);
				tourBuilder.addLeg(new Leg());
				tourBuilder.schedulePickup(carrierShipment);
			} else if (act instanceof Delivery) {
				Shipment shipment = (Shipment) ((Delivery) act).getJob();
				CarrierShipment carrierShipment = matsim2vrpMap.getCarrierShipment(shipment);
				tourBuilder.addLeg(new Leg());
				tourBuilder.scheduleDelivery(carrierShipment);
			} else if (act instanceof Start) {
				tourBuilder.scheduleStart(makeId(act.getLocationId()));
			} else if (act instanceof End) {
				tourBuilder.addLeg(new Leg());
				tourBuilder.scheduleEnd(makeId(act.getLocationId()));
			} else {
				throw new IllegalStateException("unknown tourActivity occurred. this cannot be");
			}
		}
		org.matsim.contrib.freight.carrier.Tour vehicleTour = tourBuilder.build();
		return vehicleTour;
	}
	
	static Collection<VehicleRoute> createVehicleRoutes(Collection<ScheduledTour> tours, Matsim2VrpMap matsim2vrpMap) {
		Collection<VehicleRoute> routes = new ArrayList<VehicleRoute>();
		for(ScheduledTour sTour : tours){
			VehicleRoute route = createRoute(matsim2vrpMap, sTour);
			routes.add(route);
		}
		return routes;
	}

	private static VehicleRoute createRoute(Matsim2VrpMap matsim2vrpMap,
			ScheduledTour sTour) {
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		tourBuilder.scheduleStart(sTour.getTour().getStartLinkId().toString(), sTour.getDeparture(), sTour.getDeparture());
		for(TourElement te : sTour.getTour().getTourElements()){
			if(te instanceof org.matsim.contrib.freight.carrier.Tour.TourActivity){
				org.matsim.contrib.freight.carrier.Tour.TourActivity ta = (org.matsim.contrib.freight.carrier.Tour.TourActivity) te; 
				if(ta.getActivityType().equals(FreightConstants.PICKUP)){
					org.matsim.contrib.freight.carrier.Tour.Pickup pickup = (org.matsim.contrib.freight.carrier.Tour.Pickup) ta;
					Shipment shipment = matsim2vrpMap.getShipment(pickup.getShipment());
					tourBuilder.schedulePickup(shipment);
				}
				else if(ta.getActivityType().equals(FreightConstants.DELIVERY)){
					org.matsim.contrib.freight.carrier.Tour.Delivery delivery = (org.matsim.contrib.freight.carrier.Tour.Delivery) ta;
					Shipment shipment = matsim2vrpMap.getShipment(delivery.getShipment());
					tourBuilder.scheduleDelivery(shipment);
				}
			}
		}
		tourBuilder.scheduleEnd(sTour.getTour().getEndLinkId().toString(), sTour.getVehicle().getEarliestStartTime(), sTour.getVehicle().getLatestEndTime());
		TourImpl tour = tourBuilder.build();
		VehicleRoute route = new VehicleRoute(tour,matsim2vrpMap.getVehicle(sTour.getVehicle()));
		return route;
	}

	private static Id makeId(String id) {
		return new IdImpl(id);
	}

}
