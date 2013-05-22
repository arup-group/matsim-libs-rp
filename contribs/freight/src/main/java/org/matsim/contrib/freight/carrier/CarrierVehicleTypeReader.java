package org.matsim.contrib.freight.carrier;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierVehicleType.VehicleCostInformation;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.EngineInformationImpl;
import org.xml.sax.Attributes;

/**
 * Reader reading carrierVehicleTypes from an xml-file.
 * 
 * @author sschroeder
 *
 */
public class CarrierVehicleTypeReader extends MatsimXmlParser {
	
	private static Logger logger = Logger.getLogger(CarrierVehicleTypeReader.class);
	
	private CarrierVehicleTypes carrierVehicleTypes;

	private Id currentTypeId;

	private String currentDescription;

	private Double currentWeight;

//	private Integer currentCap;

	private VehicleCostInformation currentVehicleCosts;

	private EngineInformation currentEngineInfo;

	private String currentCapacity;

	public CarrierVehicleTypeReader(CarrierVehicleTypes carrierVehicleTypes) {
		super();
		this.carrierVehicleTypes = carrierVehicleTypes;
	}
	
	/**
	 * Reads types from xml-file.
	 * 
	 * @param xml-filename containing vehicleTypes
	 */
	public void read(String filename){
		logger.info("read vehicle types");
		this.setValidating(false);
		parse(filename);
		logger.info("done");
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(name.equals("vehicleType")){
			this.currentTypeId = makeId(atts.getValue("id"));
		}
		if(name.equals("allowableWeight")){
			String weight = atts.getValue("weight");
			this.currentWeight = parseDouble(weight);
		}
		if(name.equals("engineInformation")){
			String fuelType = atts.getValue("fuelType");
			String gasConsumption = atts.getValue("gasConsumption");
			EngineInformation engineInfo = new EngineInformationImpl(parseFuelType(fuelType), parseDouble(gasConsumption));
			this.currentEngineInfo = engineInfo;
		}
		if(name.equals("costInformation")){
			String fix = atts.getValue("fix");
			String perMeter = atts.getValue("perMeter");
			String perSecond = atts.getValue("perSecond");
			if(fix == null || perMeter == null || perSecond == null) throw new IllegalStateException("cannot read costInformation correctly. probably the paramName was written wrongly");
			VehicleCostInformation vehicleCosts = new VehicleCostInformation(parseDouble(fix), parseDouble(perMeter), parseDouble(perSecond));
			this.currentVehicleCosts = vehicleCosts;
		}
	}

	private FuelType parseFuelType(String fuelType) {
		if(fuelType.equals(FuelType.diesel.toString())){
			return FuelType.diesel;
		}
		else if(fuelType.equals(FuelType.electricity.toString())){
			return FuelType.electricity;
		}
		else if(fuelType.equals(FuelType.gasoline.toString())){
			return FuelType.gasoline;
		}
		throw new IllegalStateException("fuelType " + fuelType + " is not supported");
	}

	private double parseDouble(String weight) {
		return Double.parseDouble(weight);
	}

	private Id makeId(String value) {
		return new IdImpl(value);
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equals("description")){
			this.currentDescription = content;
		}
		if(name.equals("capacity")){
			this.currentCapacity = content;
		}
		if(name.equals("vehicleType")){
			CarrierVehicleType.Builder typeBuilder = CarrierVehicleType.Builder.newInstance(currentTypeId);
			if(currentDescription != null) typeBuilder.setDescription(currentDescription);
//			if(currentWeight != null) vehType.setAllowableTotalWeight(currentWeight);
//			if(currentCap != null) vehType.setFreightCapacity(currentCap);
			if(currentVehicleCosts != null) typeBuilder.setVehicleCostInformation(currentVehicleCosts);
			if(currentEngineInfo != null) typeBuilder.setEngineInformation(currentEngineInfo);
			if(currentCapacity == null) throw new IllegalStateException("vehicleType.capacity is missing.");
			typeBuilder.setCapacity(Integer.parseInt(currentCapacity));
			CarrierVehicleType vehType = typeBuilder.build();
			carrierVehicleTypes.getVehicleTypes().put(vehType.getId(), vehType);
			reset();
		}
		
	}

	private void reset() {
		currentTypeId = null;
		currentDescription = null;
		currentWeight = null;
		currentVehicleCosts = null;
		currentEngineInfo = null;
	}

}
