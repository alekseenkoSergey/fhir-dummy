RuleSet: CiedDeviceCommon
* identifier 1..* MS
* identifier.system 1..1
* identifier.value 1..1
* status MS
* manufacturer 1..1 MS
* serialNumber 1..1 MS
* modelNumber 1..1 MS
* type 1..1 MS

Profile: IG1CiedDevice
Parent: Device
Id: ig1-cied-device
Title: "IG1 CIED Device"
Description: "Implanted cardiac device (IPG, ICD, CRT-D, CRT-P, Other). Corresponds to the CardX-CIED cied-device profile."
* insert CiedDeviceCommon
* type from IG1CiedDeviceTypeVS (required)

Profile: IG1CiedMonitor
Parent: Device
Id: ig1-cied-monitor
Title: "IG1 CIED Monitor"
Description: "Patient monitor (transmitter). Corresponds to the CardX-CIED cied-monitor profile."
* insert CiedDeviceCommon
* type = IG1DeviceTypeCS#MDC_IDC_ENUM_DEV_TYPE_Monitor
