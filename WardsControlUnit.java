package hospital_sim;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.jaamsim.ProbabilityDistributions.ExponentialDistribution;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

import hccm.controlunits.ControlUnit;
import hccm.entities.ActiveEntity;
import hccm.events.LogicEvent;

public class WardsControlUnit extends ControlUnit {
	
	@Keyword(description = "The minimum time between patient observations.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final ValueInput obsGap;
	
	
	{
		obsGap = new ValueInput("ObservationTimeGap", KEY_INPUTS, null);
		obsGap.setUnitType(TimeUnit.class);
		obsGap.setValidRange(0, Double.POSITIVE_INFINITY);
		obsGap.setRequired(true);
		this.addInput(obsGap);
		
	}
	
	public void OnStartWaitAdmission(List<ActiveEntity> ents, double simTime) {
        
        ArrayList<ActiveEntity> idleNurses = getEntitiesInSubmodelActivity("Nurse", "wait-task-nurse", simTime);
        ActivityStartCompare actSartComp = this.new ActivityStartCompare();        
        
        if (idleNurses.size() > 0) {
        	Collections.sort(idleNurses, actSartComp);
        	
        	ActiveEntity patient = ents.get(0);
        	ActiveEntity nurse = idleNurses.get(0);

        	transitionTo("admission", patient, nurse);
        }
    }
	
	public void OnStartWardStay(List<ActiveEntity> ents, double simTime) {
        ActiveEntity p = ents.get(0);
        
        ArrayList<ActiveEntity> idleNurses = this.getEntitiesInSubmodelActivity("Nurse", "wait-task-nurse", simTime);
        ActivityStartCompare actSartComp = this.new ActivityStartCompare();
        
        double obsTimeGap = obsGap.getValue();
        double lastObsTime = getNumAttribute(p, "LastObservationTime", simTime, -1);
        
        int checkObsSched = (int) getNumAttribute(p, "CheckObsScheduled", simTime, -1);
        double checkObsTime = getNumAttribute(p, "CheckObsTime", simTime, -1);
        
        int readyDischargeSched = (int) getNumAttribute(p, "ReadyDischargeSched", simTime, -1);
        double readyDischargeTime = getNumAttribute(p, "ReadyDischargeTime", simTime, -1);       
        
        if (readyDischargeSched == 0) {
        	ExponentialDistribution wardStayDist = (ExponentialDistribution) this.getSubmodelEntity("ward-stay-dist");
            double wardStayDuration = wardStayDist.getNextSample(simTime);
        	LogicEvent le = (LogicEvent) this.getSubmodelEntity("ready-for-discharge");
            le.scheduleEvent(ents, simTime + wardStayDuration);
            setNumAttribute(p, "ReadyDischargeSched", 1.0, DimensionlessUnit.class);
            setNumAttribute(p, "ReadyDischargeTime", simTime + wardStayDuration, TimeUnit.class);
        } else if (simTime > readyDischargeTime) {
        	if (idleNurses.size() > 0) {
            	Collections.sort(idleNurses, actSartComp);
            	
            	ActiveEntity nurse = idleNurses.get(0);

            	transitionTo("discharge", p, nurse);
            }
        }
        
        if (simTime > lastObsTime + obsTimeGap) {
        	OnStartReadyObservation(p.asList(), simTime);
        } else if (checkObsSched == 0) {
        	LogicEvent le = (LogicEvent) this.getSubmodelEntity("ready-for-observation");
            le.scheduleEvent(ents, lastObsTime + obsTimeGap);
            setNumAttribute(p, "CheckObsScheduled", 1.0, DimensionlessUnit.class);
            setNumAttribute(p, "CheckObsTime", lastObsTime + obsTimeGap, TimeUnit.class);

        } else if (checkObsTime < lastObsTime + obsTimeGap) {
        	LogicEvent le = (LogicEvent) this.getSubmodelEntity("ready-for-observation");
            le.rescheduleEvent(ents, lastObsTime + obsTimeGap);
            setNumAttribute(p, "CheckObsTime", lastObsTime + obsTimeGap, TimeUnit.class);
        }
    }
	
	public void OnStartWaitTest(List<ActiveEntity> ents, double simTime) {
        
        ArrayList<ActiveEntity> idleTechs = this.getEntitiesInSubmodelActivity("Technician", "wait-task-technician", simTime);
        ActivityStartCompare actSartComp = this.new ActivityStartCompare();       
        
        
        if (idleTechs.size() > 0) {
        	Collections.sort(idleTechs, actSartComp);
        	
        	ActiveEntity patient = ents.get(0);
        	ActiveEntity tech = idleTechs.get(0);

        	transitionTo("perform-test", patient, tech);
        }
    }
	
	public void OnStartReadyObservation(List<ActiveEntity> ents, double simTime) {

        ActiveEntity p = ents.get(0);
        
        String entState = p.getCurrentActivity(simTime);
        List<String> waitStates = Arrays.asList("Wards.ward-stay");;
        
        ArrayList<ActiveEntity> idleNurses = this.getEntitiesInSubmodelActivity("Nurse", "wait-task-nurse", simTime);
        ActivityStartCompare actSartComp = this.new ActivityStartCompare();
        
        
        if ((waitStates.contains(entState)) && (idleNurses.size() > 0)) {
        	Collections.sort(idleNurses, actSartComp);
        	
        	ActiveEntity patient = ents.get(0);
        	ActiveEntity nurse = idleNurses.get(0);

        	transitionTo("observation", patient, nurse);
        }
    }
	
	public void OnStartReadyDischarge(List<ActiveEntity> ents, double simTime) {

        ActiveEntity p = ents.get(0);
        
        String entState = p.getPresentState(simTime);
        List<String> waitStates = Arrays.asList("Wards.ward-stay");;
        
        ArrayList<ActiveEntity> idleNurses = this.getEntitiesInSubmodelActivity("Nurse", "wait-task-nurse", simTime);
        ActivityStartCompare actSartComp = this.new ActivityStartCompare();
        
        
        if ((waitStates.contains(entState)) && (idleNurses.size() > 0)) {
        	Collections.sort(idleNurses, actSartComp);
        	
        	ActiveEntity patient = ents.get(0);
        	ActiveEntity nurse = idleNurses.get(0);

        	transitionTo("discharge", patient, nurse);
        }
    }
	
	public void OnStartWaitTaskNurse(List<ActiveEntity> ents, double simTime) {
        ArrayList<ActiveEntity> wardStayPats = this.getEntitiesInSubmodelActivity("Patient", "ward-stay", simTime);
        ArrayList<ActiveEntity> waitDischPats = new ArrayList<ActiveEntity>();
        double readyDischTime = 0.0;
        ArrayList<ActiveEntity> waitObsPats = new ArrayList<ActiveEntity>();
        double lastObsTime = 0.0;
        double obsTimeGap = obsGap.getValue();
        for (ActiveEntity posPat : wardStayPats) {
        	readyDischTime = getNumAttribute(posPat, "ReadyDischargeTime", simTime, -1);
        	if (simTime > readyDischTime) {
        		waitDischPats.add(posPat);
        	}
        	lastObsTime = getNumAttribute(posPat, "LastObservationTime", simTime, -1);
        	if (simTime > lastObsTime + obsTimeGap) {
        		waitObsPats.add(posPat);
        	}
        }
        ArrayList<ActiveEntity> waitAdmissionPats = this.getEntitiesInSubmodelActivity("Patient", "wait-for-admission", simTime);
        ActivityStartCompare actSartComp = this.new ActivityStartCompare();
        AttributeCompare attComp2 = new AttributeCompare("LastObservationTime");
        AttributeCompare attComp3 = new AttributeCompare("ReadyDischargeTime");
        
        if (waitDischPats.size() > 0) {
        	Collections.sort(waitDischPats, attComp3);
        	
        	ActiveEntity nurse = ents.get(0);
        	ActiveEntity patient = waitDischPats.get(0);

        	transitionTo("discharge", patient, nurse);
        } else if (waitAdmissionPats.size() > 0) {
        	Collections.sort(waitAdmissionPats, actSartComp);
        	
        	ActiveEntity nurse = ents.get(0);
        	ActiveEntity patient = waitAdmissionPats.get(0);

        	transitionTo("admission", patient, nurse);
        } else if (waitObsPats.size() > 0) {
        	Collections.sort(waitObsPats, attComp2);
        	
        	ActiveEntity nurse = ents.get(0);
        	ActiveEntity patient = waitObsPats.get(0);

        	transitionTo("observation", patient, nurse);
        } 
    }
	
	public void OnStartWaitTaskTechnician(List<ActiveEntity> ents, double simTime) {
        ArrayList<ActiveEntity> waitTestPats = this.getEntitiesInSubmodelActivity("Patient", "wait-for-test", simTime);
        ActivityStartCompare actSartComp = this.new ActivityStartCompare();
        
        if (waitTestPats.size() > 0) {
        	Collections.sort(waitTestPats, actSartComp);
        	double priority = 0.0;
        	ActiveEntity tech = ents.get(0);
        	ActiveEntity patient = waitTestPats.get(0);
        	outerloop:
            	for(int i=1; i<6; i++) {
            		for(ActiveEntity testpat:waitTestPats) {
            			priority=getNumAttribute(testpat,"TriageCategory",simTime,-1);
            			if(priority==i) {
            				patient=testpat;
            				break outerloop;
            			}
            		}
            	}
        	
        	transitionTo("perform-test", patient, tech);
        } 
    }
	
	@Output(name = "ObsGapTime",
			 description = "The minimum time between observations.",
			 unitType = TimeUnit.class,
    	     sequence = 1)
	public Double getObsGapTime(double simTime) {
		return obsGap.getValue();
	}
	
    
}