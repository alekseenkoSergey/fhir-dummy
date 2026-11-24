# fhir-dummy

Java 21 / Spring Boot application with a HAPI FHIR R5 (5.0.0) client.
Set FHIR_SERVER_BASE_URL to use a different FHIR R5 server. The default is
https://hapi.fhir.org/baseR5.

Device create and update requests accept the R5 status codes active,
inactive and entered-in-error.

## IG1 validation package

After editing the FSH profiles, use SUSHI and the HL7 IG Publisher to rebuild the
package used by the application. Run these commands from the repository root.

sh
(cd ig1 && bash _build.sh -generation-off) &&
cp ig1/output/package.tgz src/main/resources/ig1/package.tgz


## Test coverage

Run ./mvnw clean verify with JDK 21. JaCoCo writes the HTML report to
target/site/jacoco/index.html and the XML report to target/site/jacoco/jacoco.xml.

## SonarQube

Create a project with the key fhir-dummy on your SonarQube server. Set
SONAR_HOST_URL to the server URL and supply a token with Execute Analysis
permission through the SONAR_TOKEN environment variable.

sh
./mvnw clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar


The scanner imports the JaCoCo XML report. Open the project's Measures page
in SonarQube to view Coverage.

## Description

- Controllers: CiedPatientController, CiedDeviceController, and CiedObservationController expose endpoints for reading patients and implanted devices and searching connectivity observations. They validate request parameters, serialize responses as FHIR JSON, and return errors as OperationOutcome resources. CiedMetadataController publishes server capability statement.

- Services: CiedPatientService, CiedDeviceService, and CiedObservationService coordinate data retrieval, mapping, and profile validation. CiedPatientService and CiedDeviceService return individual validated resources, while CiedObservationService builds search bundles with optional referenced patients, monitors, and implanted devices.

- Mappers: CiedPatientMapper, CiedDeviceMapper, CiedMonitorMapper, and CiedConnectivityStatusMapper translate upstream JSON into FHIR Patient, Device, and Observation resources. They map data, device details, connectivity statuses, and communication dates, and create consistent identifiers and resource references.

- Utilities and metadata rendering: Constants, Ids, TextUtils, and ProfileValidation centralize shared constants, identifier construction, JSON field handling, profile URLs, and validation error extraction. CiedCapabilityStatementDto generates a readable XHTML representation of the server’s capabilities.