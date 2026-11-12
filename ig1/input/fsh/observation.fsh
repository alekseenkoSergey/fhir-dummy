Profile: IG1CiedConnectivityObservation
Parent: Observation
Id: ig1-cied-connectivity-observation
Title: "IG1 CIED Connectivity Observation"
Description: "Remote-monitoring connectivity of a patient's CIED."
* status 1..1 MS
* code 1..1 MS
* code = IG1ConnectivityCS#connectivity
// Merlin.net number -> Patient/{id}
* subject 1..1 MS
* subject only Reference(IG1Patient)
* subject.reference 1..1
// patientDevices.patientDeviceId -> Device/{id}
* device 1..1 MS
* device only Reference(IG1CiedDevice or IG1CiedMonitor)
* device.reference 1..1
// patientDevices.implantDate
* effective[x] 1..1 MS
* effective[x] only dateTime or Period
* value[x] 0..0

* component MS
* component ^slicing.discriminator.type = #value
* component ^slicing.discriminator.path = "code"
* component ^slicing.rules = #closed
* component contains
    connectivityStatus 1..1 and
    connectivityModifier 0..1 and
    statusReason 0..1 and
    statusGuidance 0..1 and
    lastCiedConnectivityDate 0..1 and
    lastMonitorConnectivityDate 0..1 and
    nextCiedConnectivityDate 0..1 and
    nextMonitorConnectivityDate 0..1 and
    lastRemoteInterrogationDate 0..1 and
    nextScheduledRemoteInterrogationDate 0..1 and
    nextSecondaryScheduledRemoteInterrogationDate 0..1 and
    connectionInterval 0..1 and
    expectedTimePeriod 0..1

* component[connectivityStatus].code = IG1ConnectivityCS#connectivityStatus
* component[connectivityStatus].value[x] only CodeableConcept
* component[connectivityStatus].value[x] 1..1
* component[connectivityStatus].valueCodeableConcept from IG1ConnectivityStatusVS (required)

* component[connectivityModifier].code = IG1ConnectivityCS#connectivityModifier
* component[connectivityModifier].value[x] only CodeableConcept
* component[connectivityModifier].value[x] 1..1
* component[connectivityModifier].valueCodeableConcept from IG1ConnectivityModifierVS (required)

* component[statusReason].code = IG1ConnectivityCS#statusReason
* component[statusReason].value[x] only string
* component[statusReason].value[x] 1..1

* component[statusGuidance].code = IG1ConnectivityCS#statusGuidance
* component[statusGuidance].value[x] only string
* component[statusGuidance].value[x] 1..1

* component[lastCiedConnectivityDate].code = IG1ConnectivityCS#lastCiedConnectivityDate
* component[lastCiedConnectivityDate].value[x] only dateTime
* component[lastCiedConnectivityDate].value[x] 1..1

* component[lastMonitorConnectivityDate].code = IG1ConnectivityCS#lastMonitorConnectivityDate
* component[lastMonitorConnectivityDate].value[x] only dateTime
* component[lastMonitorConnectivityDate].value[x] 1..1

* component[nextCiedConnectivityDate].code = IG1ConnectivityCS#nextCiedConnectivityDate
* component[nextCiedConnectivityDate].value[x] only dateTime
* component[nextCiedConnectivityDate].value[x] 1..1

* component[nextMonitorConnectivityDate].code = IG1ConnectivityCS#nextMonitorConnectivityDate
* component[nextMonitorConnectivityDate].value[x] only dateTime
* component[nextMonitorConnectivityDate].value[x] 1..1

* component[lastRemoteInterrogationDate].code = IG1ConnectivityCS#lastRemoteInterrogationDate
* component[lastRemoteInterrogationDate].value[x] only dateTime
* component[lastRemoteInterrogationDate].value[x] 1..1

* component[nextScheduledRemoteInterrogationDate].code = IG1ConnectivityCS#nextScheduledRemoteInterrogationDate
* component[nextScheduledRemoteInterrogationDate].value[x] only dateTime
* component[nextScheduledRemoteInterrogationDate].value[x] 1..1

* component[nextSecondaryScheduledRemoteInterrogationDate].code = IG1ConnectivityCS#nextSecondaryScheduledRemoteInterrogationDate
* component[nextSecondaryScheduledRemoteInterrogationDate].value[x] only dateTime
* component[nextSecondaryScheduledRemoteInterrogationDate].value[x] 1..1

* component[connectionInterval].code = IG1ConnectivityCS#connectionInterval
* component[connectionInterval].value[x] only Quantity
* component[connectionInterval].value[x] 1..1
* component[connectionInterval].valueQuantity.value 1..1
* component[connectionInterval].valueQuantity.system 1..1
* component[connectionInterval].valueQuantity.system = $UCUM
* component[connectionInterval].valueQuantity.code 1..1
* component[connectionInterval].valueQuantity.code = #d
* component[connectionInterval].valueQuantity.unit = "days"

* component[expectedTimePeriod].code = IG1ConnectivityCS#expectedTimePeriod
* component[expectedTimePeriod].value[x] only Quantity
* component[expectedTimePeriod].value[x] 1..1
* component[expectedTimePeriod].valueQuantity.value 1..1
* component[expectedTimePeriod].valueQuantity.system 1..1
* component[expectedTimePeriod].valueQuantity.system = $UCUM
* component[expectedTimePeriod].valueQuantity.code 1..1
* component[expectedTimePeriod].valueQuantity.code = #d
* component[expectedTimePeriod].valueQuantity.unit = "days"
