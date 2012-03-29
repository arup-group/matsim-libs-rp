/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package org.matsim.vis.otfvis.opengl.layer;

import java.awt.Color;

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo.AgentState;

/**
 * yyyy Despite its name, this is not a Drawer but a Receiver. It receives agents 
 * (as declared by its interface but not by its name) and pushes them towards the AgentPointLayer.  
 * That AgentPointLayer has its own (Array)Drawer.  The class here cannot be renamed because of the 
 * ConnectionManager/mvi issue.  kai, feb'11
 */
public class AgentPointDrawer {

	private final OGLAgentPointLayer oglAgentPointLayer;
	
	private static OTFOGLDrawer.FastColorizer redToGreenColorizer = new OTFOGLDrawer.FastColorizer(
			new double[] { 0.0, 30., 50.}, new Color[] {Color.RED, Color.YELLOW, Color.GREEN});

	AgentPointDrawer(OGLAgentPointLayer oglAgentPointLayer) {
		this.oglAgentPointLayer = oglAgentPointLayer;
	}

	private static int bvg2cnt = 0 ;
	public void setAgent( AgentSnapshotInfo agInfo ) {
		char[] id = agInfo.getId().toString().toCharArray();
		
		if ( OTFClientControl.getInstance().getOTFVisConfig().getColoringScheme().equals( OTFVisConfigGroup.ColoringScheme.bvg ) ) {

			if ( agInfo.getAgentState()==AgentState.PERSON_DRIVING_CAR ) {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.DARK_GRAY, true);
			} else if ( agInfo.getAgentState()==AgentState.PERSON_AT_ACTIVITY ) {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.ORANGE, true);
			} else if ( agInfo.getAgentState()==AgentState.TRANSIT_DRIVER ) {
				String idstr = agInfo.getId().toString();
				if ( idstr.contains("line_B")) {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.MAGENTA, true);
				} else if ( idstr.contains("line_T")) {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.RED, true);
				} else if ( idstr.contains("line_S")) {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.GREEN, true);
				} else if ( idstr.contains("line_U")) {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.BLUE, true);
				} else {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.ORANGE, true);
				}
			} else {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.YELLOW, true);
			}
		} else if ( OTFClientControl.getInstance().getOTFVisConfig().getColoringScheme().equals( OTFVisConfigGroup.ColoringScheme.bvg2 ) ) {
			if ( bvg2cnt < 1 ) {
				bvg2cnt++ ;
				Logger.getLogger(this.getClass()).info( "using bvg2 coloring scheme ...") ;
			}

			if ( agInfo.getAgentState()==AgentState.PERSON_DRIVING_CAR ) {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.DARK_GRAY, true);
			} else if ( agInfo.getAgentState()==AgentState.PERSON_AT_ACTIVITY ) {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.ORANGE, true);
			} else if ( agInfo.getAgentState()==AgentState.TRANSIT_DRIVER ) {
				String idstr = agInfo.getId().toString();
				if ( idstr.contains("line_") && idstr.contains("-B-") ) {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.MAGENTA, true);
				} else if ( idstr.contains("line_") && idstr.contains("-T-")) {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.RED, true);
				} else if ( idstr.contains("line_SB")) {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.GREEN, true);
				} else if ( idstr.contains("line_U")) {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.BLUE, true);
				} else {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.ORANGE, true);
				}
			} else {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.YELLOW, true);
			}
		} else if ( OTFClientControl.getInstance().getOTFVisConfig().getColoringScheme().equals( OTFVisConfigGroup.ColoringScheme.byId ) ) {
			String idstr = agInfo.getId().toString() ;
			int val = 8 ;
			Color color = null ;
			if ( idstr.hashCode()%val==0 ) {
				color = Color.red ;
			} else if (idstr.hashCode()%val==1 ) {
				color = Color.orange ;
			} else if (idstr.hashCode()%val==2 ) {
				color = Color.yellow ;
			} else if (idstr.hashCode()%val==3 ) {
				color = Color.green ;
			} else if (idstr.hashCode()%val==4 ) {
				color = Color.blue ;
			} else if (idstr.hashCode()%val==5 ) {
				color = Color.cyan ;
			} else if (idstr.hashCode()%val==6 ) {
				color = Color.magenta ;
			} else if (idstr.hashCode()%val==7 ) {
				color = Color.pink ;
			} 
			this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), color, true);

		} else {
		
			if ( agInfo.getAgentState()==AgentState.PERSON_DRIVING_CAR ) {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), redToGreenColorizer.getColorZeroOne(agInfo.getColorValueBetweenZeroAndOne()), true);
			} else if ( agInfo.getAgentState()==AgentState.PERSON_AT_ACTIVITY ) {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.ORANGE, true);
			} else if ( agInfo.getAgentState()==AgentState.PERSON_OTHER_MODE ) {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.MAGENTA, true);
			} else if ( agInfo.getAgentState()==AgentState.TRANSIT_DRIVER ) {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.BLUE, true);
			} else {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.YELLOW, true);
			}

		}

	}

}