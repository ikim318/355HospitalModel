require(dplyr)
setwd("C:\\Users\\jorda\\Coding\\Java\\ENGSCI355\\hospital_sim\\Backups")

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
# patientData = read.table("complete_model-patient-event-logger.log", sep="\t",
#                      col.names=c("SimTime", "Scenario",
#                                  "Replication", "Object", "Event", "EventTime"),
#                      skip=15, na.strings=nullStrings, skipNul=TRUE)
patientData = read.table("complete_model-patient-event-logger-non-optimized-no-priority.log", sep="\t",
                         col.names=c("SimTime", "Scenario",
                                     "Replication", "Object", "Event", "EventTime"),
                         skip=15, na.strings=nullStrings, skipNul=TRUE)

# Take out warmup time
WARMUP_TIME = 15 #hours
patientsList <- patientData[patientData$SimTime >= WARMUP_TIME, ]


# Arrival time to discharge/finish - Jordan
arrival_time <-  patientData %>% group_by(Scenario, Replication, Object) %>% slice(1) %>% ungroup() %>% na.omit()
leave_time <- patientData %>% group_by(Scenario, Replication, Object) %>% filter(Event == "Wards.ward-stay" | Event =="patient-leave") %>% slice(1) %>% ungroup() %>% na.omit()
arrival_time <- arrival_time %>% mutate(time_to_discharge_or_finish = (leave_time$EventTime - arrival_time$EventTime))
# percentile1 = quantile(time_to_discharge_or_finish, probs=0.95)
# percentile1
# 5.634617

replication_list1 <- split(arrival_time, arrival_time$Replication)
percentile11 <- numeric(5)
for(i in 1:5) {
  percentile11[i] = quantile(replication_list1[[i]]$time_to_discharge_or_finish, probs=0.95, na.rm=TRUE)
}
percentile1 <- t.test(percentile11)$conf.int
# ALL 5.63

# plot(leave_time$SimTime, time_to_discharge_or_finish, type = "l", col = "blue", lwd = 2, xlab = "Simulation Time", ylab = "Time in ED", main = "Time spent in the ED by a patient against simulation running time")


# Time between needing to be observed and starting observation in ED

# Wait For Consultation
EDObservationTimes1 <- patientData %>% group_by(Scenario, Replication, Object)  %>% filter((Event == "ED.wait-for-consultation" & lead(Event) == "ED.observation") | (Event == "ED.observation" & lag(Event) == "ED.wait-for-consultation"))  %>% mutate(WaitingTimes = ifelse(Event == "ED.wait-for-consultation", (lead(EventTime) - EventTime - 0.5) * 3600,-1)) %>%  filter(Event == "ED.wait-for-consultation") %>% mutate(WaitingTimes = ifelse(WaitingTimes > 0, WaitingTimes, 0)) %>% ungroup()
# Wait For Test Results
EDObservationTimes2 <- patientData %>% group_by(Scenario, Replication, Object)  %>% filter((Event == "ED.wait-for-tests" & lead(Event) == "ED.observation") | (Event == "ED.observation" & lag(Event) == "ED.wait-for-tests"))  %>% mutate(WaitingTimes = ifelse(Event == "ED.wait-for-tests", (lead(EventTime) - EventTime - 0.5) * 3600,-1)) %>%  filter(Event == "ED.wait-for-tests") %>% mutate(WaitingTimes = ifelse(WaitingTimes > 0, WaitingTimes, 0)) %>% ungroup()
# Wait For Second Consultation
EDObservationTimes3 <- patientData %>% group_by(Scenario, Replication, Object)  %>% filter((Event == "ED.wait-for-second-consultation" & lead(Event) == "ED.observation" & lag(Event == "ED.observation")) | (Event == "ED.observation" & lag(Event) == "ED.wait-for-second-consultation" & lag(Event, n=2) == "ED.observation"))  %>% mutate(WaitingTimes = ifelse(Event == "ED.wait-for-second-consultation", (lead(EventTime) - EventTime - 0.5) * 3600,-1)) %>%  filter(Event == "ED.wait-for-second-consultation") %>% mutate(WaitingTimes = ifelse(WaitingTimes > 0, WaitingTimes, 0)) %>% ungroup()
# Niche Case of Wait For Second Consultation where there are multiple wait events in between.
EDObservationTimes4 <- patientData %>% group_by(Scenario, Replication, Object)  %>% filter((Event == "ED.wait-for-second-consultation" & lead(Event) == "ED.observation" & lag(Event == "ED.wait-for-tests")) | (Event == "ED.observation" & lag(Event) == "ED.wait-for-second-consultation" & lag(Event, n=2) == "ED.wait-for-tests"))  %>% mutate(WaitingTimes = ifelse(Event == "ED.wait-for-second-consultation", (lead(EventTime) - lag(EventTime) - 0.5) * 3600,-1)) %>%  filter(Event == "ED.wait-for-second-consultation") %>% mutate(WaitingTimes = ifelse(WaitingTimes > 0, WaitingTimes, 0)) %>% ungroup()


EDObservationTimes <- rbind(EDObservationTimes1, EDObservationTimes2, EDObservationTimes3, EDObservationTimes4)
EDObservationTimes <- EDObservationTimes %>% na.omit()
average2 <- t.test(EDObservationTimes$WaitingTimes)$conf.int
average2
# 56.80273


# Request Transit and start being picked up
request_transit_times <- patientData[patientData$Event == "PatientTransit.wait-for-assignment",]
pickup_times <- patientData[patientData$Event =="PatientTransit.pickup",]
waiting_time <- (pickup_times$EventTime - request_transit_times$EventTime) * 60
average3 <- t.test(waiting_time)$conf.int
average3
# 9.594097

# Time between needing to be observed and starting observation in Ward Should be less than  15mins 95% of the time.

# Don't take out the 0's: 12.35
wardObservationTimes <- patientData %>% group_by(Scenario, Replication, Object)  %>% filter((Event == "Wards.ward-stay" & lead(Event) == "Wards.observation") | Event == "Wards.observation" )  %>% mutate(WaitingTimes = (lead(EventTime) - EventTime - 2) * 60) %>%  filter(Event == "Wards.ward-stay") %>% ungroup() %>% na.omit()
replication_list <- split(wardObservationTimes, wardObservationTimes$Replication)
percentile44 <- numeric(5)
for(i in 1:5) {
  percentile44[i] = quantile(replication_list[[i]]$WaitingTimes, probs=0.95, na.rm=TRUE)
}
percentile4 <- t.test(percentile44)$conf.int
# ALL 12.4
# percentile4
# 12.35349

# Time Waiting for Test - Jordan and Michael did in the lab
test_begin_start <- patientData[patientData$Event == "Wards.wait-for-test",]
test_begin_end <- patientData[patientData$Event == "Wards.perform-test",]
test_duration <- (test_begin_end$EventTime - test_begin_start$EventTime) * 60
average5 <- t.test(test_duration)$conf.int
average5
# 6.227792

print(percentile1)
print(average2)
print(average3)
print(percentile4)
print(average5)

