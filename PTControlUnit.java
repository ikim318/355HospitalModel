package hospital_sim;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

import hccm.activities.ProcessActivity;
import hccm.controlunits.ControlUnit;
import hccm.entities.ActiveEntity;
import hccm.events.LogicEvent;

public class PTControlUnit extends ControlUnit {
	
	@Keyword(description = "The time an orderly waits at a dropoff before travelling back to the base.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final ValueInput dropWait;
	
	{
		dropWait = new ValueInput("DropoffWaitTime", KEY_INPUTS, null);
		dropWait.setUnitType(TimeUnit.class);
		dropWait.setValidRange(0, Double.POSITIVE_INFINITY);
		dropWait.setRequired(true);
		this.addInput(dropWait);
				
	}
	
	public class WaitingPatientCompare implements Comparator<ActiveEntity> {
		
		@Override
		public int compare(ActiveEntity ae1, ActiveEntity ae2) {
			double simTime = getSimTime();
			
			double val1 = getNumAttribute(ae1, "TriageCategory", simTime, -1);
			double val2 = getNumAttribute(ae2, "TriageCategory", simTime, -1);
			
			int ret = Double.compare(val1, val2);
			
			if (ret == 0) {
				val1 = getNumAttribute(ae1, "CurrentActivityStart", simTime, -1);
				val2 = getNumAttribute(ae2, "CurrentActivityStart", simTime, -1);
				ret = Double.compare(val1, val2);
			}
			
			return ret; 
		}		
	}
	
	public void OnStartWaitAssignment(List<ActiveEntity> ents, double simTime) {
        this.dispatchOrderlies();
    }
	
	public void OnStartWaitTaskOrderly(List<ActiveEntity> ents, double simTime) {
        this.dispatchOrderlies();
    }
	
	public void OnStartWaitDropoff(List<ActiveEntity> ents, double simTime) {
    	
    	double w_t = dropWait.getValue();
    	
        LogicEvent le = (LogicEvent) this.getSubmodelEntity("check-wait-dropoff");
        le.scheduleEvent(ents, simTime + w_t);
        this.dispatchOrderlies();
    }
	
	public void OnStartCheckWaitDropoff(List<ActiveEntity> ents, double simTime) {

		ActiveEntity checkOrderly = ents.get(0);
		double ttbSched = getNumAttribute(checkOrderly, "TTBScheduled", simTime, -1);
		if (ttbSched == 1) {
			checkOrderly.getCurrentActivity().finish(checkOrderly.asList());
						
			String ordLoc = checkOrderly.getStringAttribute("CurrentLocation", simTime);
			checkOrderly.setStringAttribute("OrdStartLoc", ordLoc);
			String patLoc = (String) "None";
			checkOrderly.setStringAttribute("PatStartLoc", patLoc);
			String patDest = (String) "OrderlyBase";
			checkOrderly.setStringAttribute("Dest", patDest);
						
			((ProcessActivity) this.getSubmodelEntity("travel-to-base")).start(checkOrderly.asList());
		}
	}

	public void dispatchOrderlies() {
		double simTime = getSimTime();
		ArrayList<ActiveEntity> waitPats = getEntitiesInSubmodelActivity("Patient", "wait-for-assignment", simTime);
		ArrayList<ActiveEntity> waitTaskOrds = getEntitiesInSubmodelActivity("Orderly", "wait-task-orderly", simTime);
		waitTaskOrds.addAll(getEntitiesInSubmodelActivity("Orderly", "wait-at-dropoff", simTime));
		
		int numWaitPats = waitPats.size();
		int numWaitOrds = waitTaskOrds.size();
		
		while ((numWaitPats > 0) & (numWaitOrds > 0)) {
			// Find highest priority patient
			WaitingPatientCompare patComp = new WaitingPatientCompare();
			Collections.sort(waitPats, patComp);
			ActiveEntity pickupPatient = waitPats.get(0);
			// Find orderly
			ActivityStartCompare actSartComp = this.new ActivityStartCompare();
			Collections.sort(waitTaskOrds, actSartComp);
			ActiveEntity assignedOrderly = waitTaskOrds.get(0);
			// Assign orderly to patient
			int ordID = (int) getNumAttribute(assignedOrderly, "ID", simTime, -1);
			setNumAttribute(pickupPatient, "AssignedOrderlyID", ordID, DimensionlessUnit.class);
			int patID = (int) getNumAttribute(pickupPatient, "ID", simTime, -1);
			setNumAttribute(assignedOrderly, "AssignedPatientID", patID, DimensionlessUnit.class);
			String ordLoc = getStringAttribute(assignedOrderly, "CurrentLocation", simTime);
			setStringAttribute(assignedOrderly, "OrdStartLoc", ordLoc);
			String patLoc = getStringAttribute(pickupPatient, "StartLocation", simTime);
			setStringAttribute(assignedOrderly, "PatStartLoc", patLoc);
			String patDest = getStringAttribute(pickupPatient, "Destination", simTime);
			setStringAttribute(assignedOrderly, "Dest", patDest);
        	
        	transitionTo("orderly-travel-to-patient", pickupPatient, assignedOrderly);
						
			numWaitPats = numWaitPats - 1;
			numWaitOrds = numWaitOrds - 1;
		}
		
	}
    
}