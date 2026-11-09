Profile: IG1HeartRateObservation
Parent: Observation
Id: ig1-heart-rate-observation
Title: "IG1 Heart Rate Observation"
Description: "Vital-sign heart rate, expressed in beats per minute."
* status MS
* category 1..1
* category = http://terminology.hl7.org/CodeSystem/observation-category#vital-signs
* code = http://loinc.org#8867-4 "Heart rate"
* subject 1..1
* subject only Reference(IG1Patient)
* device only Reference(IG1Device)
* effective[x] only dateTime
* effective[x] 1..1
* value[x] only Quantity
* value[x] 1..1
* valueQuantity.value 1..1
* valueQuantity.system 1..1
* valueQuantity.system = "http://unitsofmeasure.org"
* valueQuantity.code 1..1
* valueQuantity.code = #/min
* valueQuantity.unit = "beats/minute"