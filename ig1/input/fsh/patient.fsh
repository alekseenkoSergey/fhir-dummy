Invariant: ig1-birthdate-full
Description: "birthDate must be a full date in YYYY-MM-DD format."
Severity: #error
Expression: "toString().matches('^[0-9]{4}-[0-9]{2}-[0-9]{2}$')"

Profile: IG1Patient
Parent: Patient
Id: ig1-patient
Title: "IG1 Patient"
Description: "Patient as exchanged with Merlin.net."
// Merlin.net number
* identifier MS
* identifier ^slicing.discriminator.type = #pattern
* identifier ^slicing.discriminator.path = "system"
* identifier ^slicing.rules = #open
* identifier contains merlinNet 1..1 MS
* identifier[merlinNet] ^short = "Merlin.net number"
* identifier[merlinNet].system 1..1
* identifier[merlinNet].system = "http://example.org/fhir/ig1/NamingSystem/merlin-net-number"
* identifier[merlinNet].value 1..1
// patientStatus
* active 1..1 MS
// firstName = given[0], middleName = given[1], lastName = family
* name 1..1 MS
* name.family 1..1 MS
* name.given 1..2 MS
* name.given ^short = "given[0] = first name, given[1] = middle name (optional)"
// gender: male | female | other | unknown (required binding from base)
* gender 1..1 MS
// Phone.phoneNum
* telecom 0..1 MS
* telecom.system 1..1
* telecom.system = #phone
* telecom.use 1..1
* telecom.use = #mobile
* telecom.value 1..1
// dateOfBirthStr
* birthDate 1..1 MS
* birthDate obeys ig1-birthdate-full
