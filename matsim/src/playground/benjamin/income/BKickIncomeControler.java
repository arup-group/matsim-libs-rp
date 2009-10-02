/* *********************************************************************** *
 * project: org.matsim.*
 * BKickControler2
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.benjamin.income;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanCalcType;


/**
 * Controler for first zurich scenario test run of estimated scoring function.
 * @author dgrether
 *
 */
public class BKickIncomeControler extends Controler {

	private PersonHouseholdMapping hhdb;

	public BKickIncomeControler(String configFileName) {
		super(configFileName);
	}
	
	public BKickIncomeControler(Config conf){
		super(conf);
	}

	public BKickIncomeControler(String[] args) {
		super(args);
	}

	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		return new BKickIncomeScoringFunctionFactory(this.config.charyparNagelScoring(), this.hhdb);
	}
	
	@Override
	protected void setUp(){	
		this.hhdb = new PersonHouseholdMapping(this.scenarioData.getHouseholds());
		if (this.travelTimeCalculator == null) {
			this.travelTimeCalculator = TravelTimeCalculatorFactory.createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
		}
		this.travelCostCalculator = new BKickIncomeTravelTimeDistanceCostCalculator(this.travelTimeCalculator, this.config.charyparNagelScoring());
		super.setUp();
	}
	
	@Override
	public PlanAlgorithm getRoutingAlgorithm(final TravelCost travelCosts, final TravelTime travelTimes) {
		return new IncomePlansCalcRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes, this.getLeastCostPathCalculatorFactory(), this.hhdb);
	}
	

	@Override
	protected PopulationImpl loadPopulation() {
		PopulationImpl pop = super.loadPopulation();
		new PlanCalcType().run(pop);
		return pop;
	}
	
	public static void main(String[] args) {
//		String config = DgPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/config.xml"; //can also be included in runConfigurations/arguments/programArguments
//		String[] args2 = {config};
//		args = args2;
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new BKickIncomeControler(args);
			controler.run();
		}
	}

}
