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
patientData1 <- patientData
arrival_time <-  patientData1 %>% group_by(Scenario, Replication, Object) %>% slice(1) %>% ungroup() %>% na.omit()
leave_time <- patientData1 %>% group_by(Scenario, Replication, Object) %>% filter(Event == "Wards.ward-stay" | Event =="patient-leave") %>% slice(1) %>% ungroup() %>% na.omit()
time_to_discharge_or_finish = leave_time$EventTime - arrival_time$EventTime
percentile1 = quantile(time_to_discharge_or_finish, probs=0.95)
percentile1
# 5.634617
# arrival_time<-patientData%>%
#   group_by(Object)%>%
#   arrange(EventTime)%>%
#   arrange(Replication)%>%
#   arrange(Object)%>%
#   filter(row_number()==1)
# arrival_time<-head(arrival_time,-1)
# Leave_time<-patientData%>%
#   filter(patientData$Event=="patient-leave"|patientData$Event=='Wards.ward-stay')
# leave_index<-Leave_time%>%
#   group_by(Object)%>%
#   arrange(Replication)%>%
#   arrange(Object)%>%
#   filter(row_number()==1)

# timeinsys=quantile(leave_index$EventTime-arrival_time$EventTime,0.9)
# timeinsys


# Time between needing to be observed and starting observation in ED - Claire
newdataframe <- patientData %>%
  mutate(next_eventname=lag(Event,1))

indicies_EDobs <- which(newdataframe$Event == "ED.observation" &
                        (newdataframe$next_eventname == "ED.wait-for-tests" |
                         newdataframe$next_eventname == "ED.wait-for-second-consultation" |
                         newdataframe$next_eventname == "ED.wait-for-consultation" ))

indicies_EDEvent <- indicies_EDobs-1
ED_obs<-t.test((patientData$EventTime[indicies_EDobs]-patientData$EventTime[indicies_EDEvent]-0.5)*60)$estimate
ED_obs


# Request Transit and start being picked up (orderly stuff) - Michael
start_wait <- patientData[patientData$Event == "PatientTransit.wait-for-assignment",]
pickup <- patientData[patientData$Event == "PatientTransit.pickup",]
pickup_minutes <- t.test(pickup$EventTime-start_wait$EventTime)$estimate*60
pickup_minutes

# Time between needing to be observed and starting observation in Ward
newdataframe<-patientData%>%
  mutate(next_eventname=lag(Event,1))
indicies<-which(newdataframe$Event == "Wards.observation" & newdataframe$next_eventname == "Wards.ward-stay")

indicies2<-indicies-1
ward_obs<-quantile((patientData$EventTime[indicies]-patientData$EventTime[indicies2]-2)*60,0.9)
ward_obs

# Time Waiting for Test - Jordan and Michael did in the lab
test_begin_start <- patientData[patientData$Event == "Wards.wait-for-test",]
test_begin_end <- patientData[patientData$Event == "Wards.perform-test",]
test_duration <- test_begin_end$EventTime - test_begin_start$EventTime
minutes <- t.test(test_duration)$estimate * 60
minutes


