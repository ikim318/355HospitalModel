require(dplyr)
setwd("C:") # Set wd here

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

nullStrings = c("this.SimTime/1[h]", "Scenario", "Replication", "this.obj", "Event", "EventTime")
patientData = read.table("complete_model-patient-event-logger.log", sep="\t",
                     col.names=c("SimTime", "Scenario",
                                 "Replication", "Object", "Event", "EventTime"),
                     skip=15, na.strings=nullStrings, skipNul=TRUE)
orderlyData = read.table("complete_model-PatientTransit.orderly-event-logger.log", sep="\t",
                         col.names=c("SimTime", "Scenario", "Replication", "Object", "Event", "EventTime"),
                         skip=15, na.strings=nullStrings, skipNul=TRUE)

# Arrival time to discharge/finish - Jordan
arrival_time <-  patientData %>% group_by(Scenario, Replication, Object) %>% slice(1) %>% ungroup() %>% na.omit()
leave_time <- patientData %>% group_by(Scenario, Replication, Object) %>% filter(Event == "Wards.ward-stay" | Event =="patient-leave") %>% slice(1) %>% ungroup() %>% na.omit()
time_to_discharge_or_finish = leave_time$EventTime - arrival_time$EventTime
percentile1 = quantile(time_to_discharge_or_finish, probs=0.95)
percentile1
# 5.634617

# Time between needing to be observed and starting observation in ED - Claire 54.84s?

# Wait For Consultation
EDObservationTimes1 <- patientData %>% group_by(Scenario, Replication, Object)  %>% filter((Event == "ED.wait-for-consultation" & lead(Event) == "ED.observation") | (Event == "ED.observation" & lag(Event) == "ED.wait-for-consultation"))  %>% mutate(WaitingTimes = ifelse(Event == "ED.wait-for-consultation", (lead(EventTime) - EventTime - 0.5) * 60,-1)) %>%  filter(Event == "ED.wait-for-consultation") %>% ungroup()
# Wait For Test Results
EDObservationTimes2 <- patientData %>% group_by(Scenario, Replication, Object)  %>% filter((Event == "ED.wait-for-tests" & lead(Event) == "ED.observation") | (Event == "ED.observation" & lag(Event) == "ED.wait-for-tests"))  %>% mutate(WaitingTimes = ifelse(Event == "ED.wait-for-tests", (lead(EventTime) - EventTime - 0.5) * 60,-1)) %>%  filter(Event == "ED.wait-for-tests") %>% ungroup()
# Wait For Second Consultation
EDObservationTimes3 <- patientData %>% group_by(Scenario, Replication, Object)  %>% filter((Event == "ED.wait-for-second-consultation" & lead(Event) == "ED.observation") | (Event == "ED.observation" & lag(Event) == "ED.wait-for-second-consultation"))  %>% mutate(WaitingTimes = ifelse(Event == "ED.wait-for-second-consultation", (lead(EventTime) - EventTime - 0.5) * 60,-1)) %>%  filter(Event == "ED.wait-for-second-consultation") %>% ungroup()

EDObservationTimes = rbind(EDObservationTimes1, EDObservationTimes2, EDObservationTimes3)
average2 = mean(EDObservationTimes$WaitingTimes, na.rm=TRUE) * 60 # seconds
average2
# 54.84244


# Request Transit and start being picked up (orderly stuff) # avg 12.8 (<20)
request_transit_times <- patientData[patientData$Event == "PatientTransit.wait-for-assignment",]
pickup_times <- patientData[patientData$Event =="PatientTransit.pickup",]
waiting_time <- pickup_times$EventTime - request_transit_times$EventTime
average3 <- t.test(waiting_time)$estimate * 60
average3
# 9.594097

# Time between needing to be observed and starting observation in Ward 12.35?. Should be less than  15mins 95% of the time.

# Don't take out the 0's: 12.35
wardObservationTimes <- patientData %>% group_by(Scenario, Replication, Object)  %>% filter((Event == "Wards.ward-stay" & lead(Event) == "Wards.observation") | (Event == "Wards.observation" & lag(Event) == "Wards.ward-stay"))  %>% mutate(WaitingTimes = ifelse(Event == "Wards.ward-stay", (lead(EventTime) - EventTime - 2) * 60,-1)) %>%  filter(Event == "Wards.ward-stay") %>% ungroup()
percentile4 = quantile(wardObservationTimes$WaitingTimes, probs=0.95, na.rm=TRUE)
percentile4
# 12.35349

# Time Waiting for Test - Jordan and Michael did in the lab
test_begin_start <- patientData[patientData$Event == "Wards.wait-for-test",]
test_begin_end <- patientData[patientData$Event == "Wards.perform-test",]
test_duration <- test_begin_end$EventTime - test_begin_start$EventTime
average5 <- t.test(test_duration)$estimate * 60
average5
# 6.227792

