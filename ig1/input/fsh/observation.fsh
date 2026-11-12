Profile: IG1CiedConnectivityObservation
Parent: cied-connectivity-status
Id: ig1-cied-connectivity-observation
Title: "IG1 CIED Connectivity Observation"
Description: "Remote-monitoring connectivity of a patient's CIED. Constrains the CardX-CIED cied-connectivity-status profile (FHIR R5)."
* status 1..1 MS
* code 1..1 MS
// Merlin.net number -> Patient/{id}
* subject only Reference(IG1Patient)
* subject.reference 1..1
// patientDevices.patientDeviceId -> Device/{id} (the monitor); the implanted CIED goes in the ciedDevice extension
* device 1..1 MS
* device only Reference(IG1CiedMonitor)
* device.reference 1..1
* extension[ciedDevice].value[x] only Reference(IG1CiedDevice)
* effective[x] 1..1 MS
* value[x] 0..0

* component MS
* component[connectivityStatus] 1..1
* component[connectivityModifier] 0..1

// every optional component that is present must carry a value
* component[connectivityStatus].value[x] 1..1
* component[connectivityModifier].value[x] 1..1
* component[statusReason].value[x] 1..1
* component[statusGuidance].value[x] 1..1
* component[lastCiedConnectivityDate].value[x] 1..1
* component[lastMonitorConnectivityDate].value[x] 1..1
* component[nextCiedConnectivityDate].value[x] 1..1
* component[nextMonitorConnectivityDate].value[x] 1..1
* component[lastRemoteInterrogationDate].value[x] 1..1
* component[nextScheduledRemoteInterrogationDate].value[x] 1..1
* component[nextSecondaryScheduledRemoteInterrogationDate].value[x] 1..1
// days (CardX-CIED models both as integer)
* component[connectionInterval].value[x] 1..1
* component[expectedTimePeriod].value[x] 1..1
