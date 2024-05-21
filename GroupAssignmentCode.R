require(dplyr)
#change to your wd
setwd("E:\\ENGSCI355\\assignment")

# Important files to read
# complete_model.rep?
# complete-model-patient-event-logger
# complete_model-PatientTransit.orderly-event-logger

# Performance Targets
# Arrival time to discharge/finish being admitted to ward time -> <6hrs for 95% of patients
# Time between needing to be observed and starting an observation should be <2mins on average
# Time between requesting transit (starting to wait for orderly to be assigned) and starting being picked up, should be less than 20 mins on avg
# time between needing to be obvserved and starting observation in wards, should be <15 mins 95% of the time.
# Time waiting for test - < 5 mins on avg.


# NOTE: IF YOU GET AN ERROR incomplete final line found by readTableHeader on 'complete_model-PatientTransit.orderly-event-logger.log',
# go into the log file (i.e. in notepad) and add a tab at the very end
#

nullStrings = c("this.SimTime/1[h]", "Scenario", "Replication", "this.obj", "Event", "EventTime")
patientData = read.table("complete_model-patient-event-logger.log", sep="\t",
                     col.names=c("SimTime", "Scenario",
                                 "Replication", "Object", "Event", "EventTime"),
                     skip=15, na.strings=nullStrings, skipNul=TRUE)
orderlyData = read.table("complete_model-PatientTransit.orderly-event-logger.log", sep="\t",
                         col.names=c("SimTime", "Scenario", "Replication", "Object", "Event", "EventTime"),
                         skip=15, na.strings=nullStrings, skipNul=TRUE)

# Arrival time to discharge/finish - Jordan

# Time between needing to be observed and starting observation in ED - Claire

# Request Transit and start being picked up (orderly stuff) - Michael
start_wait <- patientData[patientData$Event == "PatientTransit.wait-for-assignment",]
pickup <- patientData[patientData$Event == "PatientTransit.pickup",]
pickup_minutes <- t.test(pickup$EventTime-start_wait$EventTime)$estimate*60
pickup_minutes
# Time between needing to be observed and starting observation in Ward

# Time Waiting for Test - Jordan and Michael did in the lab
test_begin_start <- patientData[patientData$Event == "Wards.wait-for-test",]
test_begin_end <- patientData[patientData$Event == "Wards.perform-test",]
test_duration <- test_begin_end$EventTime - test_begin_start$EventTime
minutes <- t.test(test_duration)$estimate * 60
minutes


