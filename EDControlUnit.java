package hospital_sim;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.jaamsim.ProbabilityDistributions.NormalDistribution;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

import hccm.activities.WaitActivity;
import hccm.controlunits.ControlUnit;
import hccm.entities.ActiveEntity;
import hccm.events.LogicEvent;

public class EDControlUnit extends ControlUnit {
	
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
		
	public void OnStartWaitRegister(List<ActiveEntity> ents, double simTime) {
        // If there are any requests, then sort them by time
        ArrayList<ActiveEntity> idleNurses = getEntitiesInSubmodelActivity("Nurse", "wait-task-nurse", simTime);  
        ActivityStartCompare actComp = new ActivityStartCompare();
        
        if (idleNurses.size() > 0) {        	
        	Collections.sort(idleNurses, actComp);
        	ActiveEntity patient = ents.get(0);
        	ActiveEntity nurse = idleNurses.get(0);
        	transitionTo("register", patient, nurse);
        }
    }
	
	public void OnStartWaitTriage(List<ActiveEntity> ents, double simTime) {
        ArrayList<ActiveEntity> idleNurses = getEntitiesInSubmodelActivity("Nurse", "wait-task-nurse", simTime);
        ActivityStartCompare actComp = new ActivityStartCompare();
                
        if (idleNurses.size() > 0) {
        	Collections.sort(idleNurses, actComp);
        	ActiveEntity patient = ents.get(0);
        	ActiveEntity nurse = idleNurses.get(0);
        	transitionTo("triage", patient, nurse);
        }
    }
	
	public void OnStartReadyObservation(List<ActiveEntity> ents, double simTime) {
        ActiveEntity p = ents.get(0);
        
        String entAct = p.getCurrentActivity(simTime);
        List<String> waitActs = Arrays.asList("ED.wait-for-consultation", "ED.wait-for-second-consultation", "ED.wait-for-tests");
        
        ArrayList<ActiveEntity> idleNurses = getEntitiesInSubmodelActivity("Nurse", "wait-task-nurse", simTime);
        ActivityStartCompare actComp = new ActivityStartCompare();
        
        if ((waitActs.contains(entAct)) && (idleNurses.size() > 0)) {
        	Collections.sort(idleNurses, actComp);
        	ActiveEntity patient = ents.get(0);
        	ActiveEntity nurse = idleNurses.get(0);	
        	transitionTo("observation", patient, nurse);
        }
    }
	
	public void checkObsScheduling(ActiveEntity patient, double simTime) {
		
		double obsTimeGap = obsGap.getValue();
        double lastObsTime = getNumAttribute(patient, "LastObservationTime", simTime, -1);
        
        int checkObsSched = (int) getNumAttribute(patient, "CheckObsScheduled", simTime, -1);
        double checkObsTime = getNumAttribute(patient, "CheckObsTime", simTime, -1);
		
		if (simTime > lastObsTime + obsTimeGap) {
        	OnStartReadyObservation(patient.asList(), simTime);
        } else if (checkObsSched == 0) {
        	LogicEvent le = (LogicEvent) getSubmodelEntity("ready-for-observation");
            le.scheduleEvent(patient.asList(), lastObsTime + obsTimeGap);
            setNumAttribute(patient, "CheckObsScheduled", 1.0, DimensionlessUnit.class);
            setNumAttribute(patient, "CheckObsTime", lastObsTime + obsTimeGap, TimeUnit.class);
        } else if (checkObsTime < lastObsTime + obsTimeGap) {
        	LogicEvent le = (LogicEvent) getSubmodelEntity("ready-for-observation");
            le.rescheduleEvent(patient.asList(), lastObsTime + obsTimeGap);
            setNumAttribute(patient, "CheckObsTime", lastObsTime + obsTimeGap, TimeUnit.class);
        }
	}
	
	public void OnStartWaitConsultation(List<ActiveEntity> ents, double simTime) {
        ActiveEntity p = ents.get(0);
        
        ArrayList<ActiveEntity> idleDocs = getEntitiesInSubmodelActivity("Doctor", "wait-task-doctor", simTime);
        ActivityStartCompare actComp = new ActivityStartCompare();
                
        if (idleDocs.size() > 0) {
        	Collections.sort(idleDocs, actComp);
        	ActiveEntity patient = ents.get(0);
        	ActiveEntity doc = idleDocs.get(0);
        	transitionTo("consultation", patient, doc);
        } else {
        	checkObsScheduling(p, simTime);
        }
    }
	
	public void OnStartWaitSecondConsultation(List<ActiveEntity> ents, double simTime) {
        ActiveEntity p = ents.get(0);
        
        ArrayList<ActiveEntity> idleDocs = getEntitiesInSubmodelActivity("Doctor", "wait-task-doctor", simTime);
        ActivityStartCompare actComp = new ActivityStartCompare();
        
        if (idleDocs.size() > 0) {
        	Collections.sort(idleDocs, actComp);
        	ActiveEntity patient = ents.get(0);
        	ActiveEntity doc = idleDocs.get(0);
        	transitionTo("second-consultation", patient, doc);
        } else {
        	checkObsScheduling(p, simTime);
        }
    }
	
	public void OnStartWaitTestResults(List<ActiveEntity> ents, double simTime) {
        ActiveEntity p = ents.get(0);
        int resultsBackTrgSched = (int) getNumAttribute(p, "TestReturnScheduled", simTime, -1);

    	if (resultsBackTrgSched == 0) {
    		double testDelaySeconds = ((NormalDistribution) getSubmodelEntity("test-back-dist")).getNextSample(simTime);
        	setNumAttribute(p, "TestReturnScheduled", 1.0, DimensionlessUnit.class);
            setNumAttribute(p, "ResultsBackTime", simTime + testDelaySeconds, TimeUnit.class);

            LogicEvent le = (LogicEvent) getSubmodelEntity("test-results-back");
            le.scheduleEvent(ents, simTime + testDelaySeconds);
    	}
    	checkObsScheduling(p, simTime);
    }
	
	public void OnStartTestResultBack(List<ActiveEntity> ents, double simTime) {
        ActiveEntity p = ents.get(0);
        String entAct = p.getCurrentActivity(simTime);
        if (entAct.equals("ED.wait-for-tests")) {
        	p.getCurrentActivity().finish(p.asList());
        	((WaitActivity) getSubmodelEntity("wait-for-second-consultation")).addEntity(p);
        }
    }
	
	public void OnStartWaitTaskNurse(List<ActiveEntity> ents, double simTime) {
        ArrayList<ActiveEntity> waitTriagePats = getEntitiesInSubmodelActivity("Patient", "wait-for-triage", simTime);
        ArrayList<ActiveEntity> waitRegisterPats = getEntitiesInSubmodelActivity("Patient", "wait-to-register", simTime);
        ActivityStartCompare actComp = new ActivityStartCompare();
        
        double obsTimeGap = obsGap.getValue();
        double lastObsTime = 0.0;
        
        ArrayList<ActiveEntity> posObsPats = getEntitiesInSubmodelActivity("Patient", "wait-for-consultation", simTime);
        posObsPats.addAll(getEntitiesInSubmodelActivity("Patient", "wait-for-tests", simTime));
        posObsPats.addAll(getEntitiesInSubmodelActivity("Patient", "wait-for-second-consultation", simTime));
        ArrayList<ActiveEntity> waitObsPats = new ArrayList<ActiveEntity>();
        for (ActiveEntity posPat : posObsPats) {
        	lastObsTime = getNumAttribute(posPat, "LastObservationTime", simTime, -1);
        	if (simTime > lastObsTime + obsTimeGap) {
        		waitObsPats.add(posPat);
        	}
        }
        AttributeCompare attComp2 = new AttributeCompare("LastObservationTime");
        double priority =0;
        if (waitTriagePats.size() > 0) {
        	Collections.sort(waitTriagePats, actComp);
        	ActiveEntity patient = waitTriagePats.get(0);
        	outerloop:
            	for(int i=2; i<6; i++) {
            		for(ActiveEntity triagepat:waitTriagePats) {
            			priority=getNumAttribute(triagepat,"TriageCategory",simTime,-1);
            			if(priority==i) {
            				patient=triagepat;
            				break outerloop;
            			}
            		}
            	}   	
        	ActiveEntity nurse = ents.get(0);
        	transitionTo("triage", patient, nurse);
        } else if (waitObsPats.size() > 0) {
        	Collections.sort(waitObsPats, attComp2);
        	
        	ActiveEntity nurse = ents.get(0);
        	ActiveEntity patient = waitObsPats.get(0);
        	        	
        	transitionTo("observation", patient, nurse);
        } else if (waitRegisterPats.size() > 0) {
        	Collections.sort(waitRegisterPats, actComp);
        	
        	ActiveEntity nurse = ents.get(0);
        	ActiveEntity patient = waitRegisterPats.get(0);

        	transitionTo("register", patient, nurse);
        }
    }
	
	public void OnStartWaitTaskDoctor(List<ActiveEntity> ents, double simTime) {
        ArrayList<ActiveEntity> waitPats = getEntitiesInSubmodelActivities("Patient", simTime, "wait-for-consultation", "wait-for-second-consultation");
        ActivityStartCompare actSartComp = this.new ActivityStartCompare();
        
        
        if (waitPats.size() > 0) {
        	Collections.sort(waitPats, actSartComp);
        	double priority=0.0;
        	ActiveEntity doctor = ents.get(0);
        	ActiveEntity patient = waitPats.get(0);
        	outerloop:
        	for(int i=1; i<6; i++) {
        		for(ActiveEntity consultpat:waitPats) {
        			priority=getNumAttribute(consultpat,"TriageCategory",simTime,-1);
        			if(priority==i) {
        				patient=consultpat;
        				break outerloop;
        			}
        		}
        	}
        	
//        	if (patient.getNumAttribute("ID", simTime, -1) == 3) {
//        		int x = 1;
//        	}
        	String entAct = patient.getCurrentActivity(simTime);
        	if (entAct.equals("ED.wait-for-consultation")) {
        		transitionTo("consultation", patient, doctor);
        	} else {
        		transitionTo("second-consultation", patient, doctor);
        	}
        	
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