Profile: IG1Device
Parent: Device
Id: ig1-device
Title: "IG1 Device"
Description: "Device that must be identified, named and linked to a patient."
* identifier 1..* MS
* status 1..1
* deviceName 1..* MS
* deviceName.type = #user-friendly-name
* patient 1..1
* patient only Reference(IG1Patient)
