# Code for determining warm up time.

require(dplyr)
setwd("C:\\Users\\jorda\\Coding\\Java\\ENGSCI355\\hospital_sim")

nullStrings = c("this.SimTime/1[h]", "Scenario", "Replication", "this.obj", "Event", "EventTime")
# patientData = read.table("complete_model-patient-event-logger-non-optimized-no-priority.log", sep="\t",
#                          col.names=c("SimTime", "Scenario",
#                                      "Replication", "Object", "Event", "EventTime"),
#                          skip=15, na.strings=nullStrings, skipNul=TRUE)

patientData = read.table("complete_model-patient-event-logger.log", sep="\t",
                         col.names=c("SimTime", "Scenario",
                                     "Replication", "Object", "Event", "EventTime"),
                         skip=15, na.strings=nullStrings, skipNul=TRUE)

patientData$Object <- as.numeric(gsub("Patient", "", patientData$Object))
patientData1 <- patientData %>% filter(Replication == 1) %>% group_by(Scenario, Object) %>% mutate(WardAdmissionTime = ifelse(Event == "Wards.wait-for-admission", lead(EventTime) - EventTime, NA)) %>% filter(Event == "Wards.wait-for-admission") %>% ungroup() %>% na.omit()
patientData2 <- patientData %>% filter(Replication == 2) %>% group_by(Scenario, Object) %>% mutate(WardAdmissionTime = ifelse(Event == "Wards.wait-for-admission", lead(EventTime) - EventTime, NA)) %>% filter(Event == "Wards.wait-for-admission") %>% ungroup() %>% na.omit()
patientData3 <- patientData %>% filter(Replication == 3) %>% group_by(Scenario, Object) %>% mutate(WardAdmissionTime = ifelse(Event == "Wards.wait-for-admission", lead(EventTime) - EventTime, NA)) %>% filter(Event == "Wards.wait-for-admission") %>% ungroup() %>% na.omit()
patientData4 <- patientData %>% filter(Replication == 4) %>% group_by(Scenario, Object) %>% mutate(WardAdmissionTime = ifelse(Event == "Wards.wait-for-admission", lead(EventTime) - EventTime, NA)) %>% filter(Event == "Wards.wait-for-admission") %>% ungroup() %>% na.omit()
patientData5 <- patientData %>% filter(Replication == 5) %>% group_by(Scenario, Object) %>% mutate(WardAdmissionTime = ifelse(Event == "Wards.wait-for-admission", lead(EventTime) - EventTime, NA)) %>% filter(Event == "Wards.wait-for-admission") %>% ungroup() %>% na.omit()
patientData1 <- patientData1[order(patientData1$Object),]
patientData2 <- patientData1[order(patientData2$Object),]
patientData3 <- patientData1[order(patientData3$Object),]
patientData4 <- patientData1[order(patientData4$Object),]
patientData5 <- patientData1[order(patientData5$Object),]


# Create a list of data frames
df_list <- list(patientData1, patientData2, patientData3, patientData4, patientData5)
means_list <- list()
colour_list <- list("red", "yellow", "blue", "green", "orange")
# Loop through the list of data frames
for (i in seq_along(df_list)) {
  df <- df_list[[i]]
  colour = colour_list[i]

  df <- df[1:8000,]
  
  # Number of rows to batch
  batch_size <- 500
  
  # Calculate the number of batches
  num_batches <- nrow(df) %/% batch_size
  
  # Initialize vectors to store means
  mean_x <- numeric(num_batches)
  mean_y <- numeric(num_batches)
  
  # Loop through each batch to calculate means
  for (j in 1:num_batches) {
    start_index <- (j - 1) * batch_size + 1
    end_index <- j * batch_size
    batch <- df[start_index:end_index, ]
    mean_x[j] <- batch$SimTime[batch_size] # Take Max
    mean_y[j] <- mean(batch$WardAdmissionTime, na.rm=TRUE)
  }
  
  # Create a data frame with the means
  means_df <- data.frame(mean_x = mean_x, mean_y = mean_y)
  means_list[[i]] <- means_df
  # Print the means data frame
  print("Means Data Frame:")
  print(means_df)
  
}

plot(means_list[[1]]$mean_x, means_list[[1]]$mean_y, type = "l", col = 1, xlab = "SimTime", ylab = "Time Waiting For Ward Admission", main = "Warmup Time")

# Loop through the remaining data frames starting from the second one
for (i in 2:length(means_list)) {
  lines(means_list[[i]]$mean_x, means_list[[i]]$mean_y, col = i)  # Add lines for each data frame with different colors
}

# Add a legend
legend("topright", legend = paste("Replication", 1:length(means_list)), col = 1:length(means_list), lty = 1, cex = 0.8)
