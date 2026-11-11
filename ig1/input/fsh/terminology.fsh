Alias: $UCUM = http://unitsofmeasure.org

// ---------------------------------------------------------------------------
// Device types (mirrors the MDC IDC device type codes used by CardX-CIED)
// ---------------------------------------------------------------------------
CodeSystem: IG1DeviceTypeCS
Id: ig1-device-type-cs
Title: "IG1 CIED Device Types"
Description: "CIED device type codes (MDC IDC enumeration)."
* ^caseSensitive = true
* #MDC_IDC_ENUM_DEV_TYPE_IPG "IPG"
* #MDC_IDC_ENUM_DEV_TYPE_ICD "ICD"
* #MDC_IDC_ENUM_DEV_TYPE_CRT_D "CRT-D"
* #MDC_IDC_ENUM_DEV_TYPE_CRT_P "CRT-P"
* #MDC_IDC_ENUM_DEV_TYPE_Monitor "Monitor"
* #MDC_IDC_ENUM_DEV_TYPE_Other "Other"

ValueSet: IG1CiedDeviceTypeVS
Id: ig1-cied-device-type-vs
Title: "IG1 CIED Implanted Device Types"
Description: "Types allowed for the cied-device profile."
* IG1DeviceTypeCS#MDC_IDC_ENUM_DEV_TYPE_IPG
* IG1DeviceTypeCS#MDC_IDC_ENUM_DEV_TYPE_ICD
* IG1DeviceTypeCS#MDC_IDC_ENUM_DEV_TYPE_CRT_D
* IG1DeviceTypeCS#MDC_IDC_ENUM_DEV_TYPE_CRT_P
* IG1DeviceTypeCS#MDC_IDC_ENUM_DEV_TYPE_Other

// ---------------------------------------------------------------------------
// Connectivity observation codes
// ---------------------------------------------------------------------------
CodeSystem: IG1ConnectivityCS
Id: ig1-connectivity-cs
Title: "IG1 Connectivity Codes"
Description: "Codes identifying the connectivity Observation and its components, plus connectivity status/modifier values."
* ^caseSensitive = true
// Observation / component codes
* #connectivity "Device connectivity"
* #connectivityStatus "Connectivity status"
* #connectivityModifier "Connectivity modifier"
* #statusReason "Status reason"
* #statusGuidance "Status guidance"
* #lastCiedConnectivityDate "Last CIED connectivity date"
* #lastMonitorConnectivityDate "Last monitor connectivity date"
* #nextCiedConnectivityDate "Next CIED connectivity date"
* #nextMonitorConnectivityDate "Next monitor connectivity date"
* #lastRemoteInterrogationDate "Last remote interrogation date"
* #nextScheduledRemoteInterrogationDate "Next scheduled remote interrogation date"
* #nextSecondaryScheduledRemoteInterrogationDate "Next secondary scheduled remote interrogation date"
* #connectionInterval "Connection interval"
* #expectedTimePeriod "Expected time period"
// connectivityStatus values
* #not-applicable "Not applicable"
* #connected "Connected"
* #disconnected "Disconnected"
// connectivityModifier values
* #setup-not-completed "Setup not completed"
* #suspended "Suspended"
* #inactive "Inactive"
* #unenrolled "Unenrolled"
* #transferred "Transferred"

ValueSet: IG1ConnectivityStatusVS
Id: ig1-connectivity-status-vs
Title: "IG1 Connectivity Status"
Description: "Overall system connectivity status as calculated by the CIED manufacturer."
* IG1ConnectivityCS#not-applicable
* IG1ConnectivityCS#connected
* IG1ConnectivityCS#disconnected

ValueSet: IG1ConnectivityModifierVS
Id: ig1-connectivity-modifier-vs
Title: "IG1 Connectivity Modifier"
Description: "Additional modifiers to the connectivity status as calculated by the CIED manufacturer."
* IG1ConnectivityCS#setup-not-completed
* IG1ConnectivityCS#suspended
* IG1ConnectivityCS#inactive
* IG1ConnectivityCS#unenrolled
* IG1ConnectivityCS#transferred
