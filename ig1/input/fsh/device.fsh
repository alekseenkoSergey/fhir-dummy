// Fields shared by both device profiles (Merlin.net PatientDevices)
RuleSet: CiedDeviceCommon
// patientDeviceId
* identifier 1..* MS
* identifier.system 1..1
* identifier.value 1..1
// status is optional: active | inactive | entered-in-error
* status MS
* manufacturer 1..1 MS
// serialNum
* serialNumber 1..1 MS
// modelNum
* modelNumber 1..1 MS
* type 1..1 MS
* type.coding 1..* MS

Profile: IG1CiedDevice
Parent: cied-device
Id: ig1-cied-device
Title: "IG1 CIED Device"
Description: "Implanted cardiac device (IPG, ICD, CRT-D, CRT-P, Other). Constrains the CardX-CIED cied-device profile (FHIR R5). Device.type is bound to the CardX MDCIDCDeviceTypes value set."
* insert CiedDeviceCommon

Profile: IG1CiedMonitor
Parent: cied-monitor
Id: ig1-cied-monitor
Title: "IG1 CIED Monitor"
Description: "Patient monitor (transmitter). Constrains the CardX-CIED cied-monitor profile (FHIR R5). Device.type is bound to the CardX cied-monitor-types-vs value set."
* insert CiedDeviceCommon
