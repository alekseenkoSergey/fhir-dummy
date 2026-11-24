package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.example.fhirdummy.dtos.CapabilityStatementDto;
import org.example.fhirdummy.service.ObservationService;
import static org.example.fhirdummy.utility.Constants.ID_DELIMITER;
import org.example.fhirdummy.utility.ProfileValidation;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r5.model.CapabilityStatement.TypeRestfulInteraction;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.Enumerations.CapabilityStatementKind;
import org.hl7.fhir.r5.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Enumerations.SearchParamType;
import org.hl7.fhir.r5.model.Narrative;
import org.hl7.fhir.r5.model.Narrative.NarrativeStatus;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publishes the server's FHIR CapabilityStatement through the metadata endpoint.
 * It prepares JSON and HTML representations at startup and selects a response using the format parameter
 * or the request's Accept header.
 */
@RestController
public class CiedMetadataController {
    private static final MediaType FHIR_JSON = MediaType.valueOf("application/fhir+json");
    private static final MediaType HTML_UTF8 = MediaType.valueOf("text/html;charset=UTF-8");

    private final String metadataJson;
    private final String metadataHtml;

    public CiedMetadataController(FhirContext context, @Value("${fhir.public-base-url:}") String publicBaseUrl) {
        CapabilityStatement statement = new CapabilityStatement();
        statement.setName("InterOPFhirCapabilities");
        statement.setTitle("InterOP FHIR Capabilities");
        statement.setStatus(PublicationStatus.ACTIVE);
        statement.setDateElement(new DateTimeType("2026-11-24"));
        statement.setKind(CapabilityStatementKind.INSTANCE);
        statement.setDescription("FHIR R5 API for reading CIED patients and implanted devices, "
                + "and searching CIED connectivity status observations with optional related resources.");
        statement.getImplementation().setDescription("CardX - Cardiac Implantable Electronic Devices, IG1");
        if (!publicBaseUrl.isBlank()) {
            statement.getImplementation().setUrl(publicBaseUrl);
        }
        statement.setFhirVersion(FHIRVersion._5_0_0);
        statement.addFormat(FHIR_JSON.toString());
        statement.addImplementationGuide(ProfileValidation.CARDX_CIED_GUIDE);

        var server = statement.addRest()
                .setMode(RestfulCapabilityMode.SERVER)
                .setDocumentation("All resource paths are relative to the FHIR base URL (/fhir). "
                        + "Resource responses use application/fhir+json and Cache-Control: no-store. "
                        + "Supported operations are GET Patient/{id}, GET Device/{id}, and GET Observation "
                        + "with clinicId and an optional patientId. "
                        + "The listed profiles are used to validate the mapped resources; the mappings below "
                        + "do not implement every Must Support element of the IG. "
                        + "Controller-handled invalid requests return HTTP 400 with OperationOutcome; "
                        + "missing resources return HTTP 404, and upstream failures retain their HTTP error status "
                        + "with OperationOutcome (including HTTP 502 for invalid upstream data). "
                        + "GET metadata provides this CapabilityStatement as FHIR JSON or HTML: "
                        + "use _format=json or _format=html, or the Accept header; _format takes precedence. "
                        + "Unsupported metadata representations return HTTP 406.");

        var patient = server.addResource()
                .setType("Patient")
                .addSupportedProfile(ProfileValidation.PATIENT_PROFILE)
                .setDocumentation("GET Patient/{clinicId" + ID_DELIMITER
                        + "patientId} returns a CIED Patient. The optional _profile read parameter defaults to "
                        + ProfileValidation.PATIENT_PROFILE + " and accepts only that canonical URL. "
                        + "Patients can also be included in Observation search results. "
                        + "The current mapping covers identifier, name, phone, gender and birth date; "
                        + "Patient.address is not mapped, including when supplied upstream.");
        patient.addInteraction().setCode(TypeRestfulInteraction.READ);

        var device = server.addResource()
                .setType("Device")
                .addSupportedProfile(ProfileValidation.DEVICE_PROFILE)
                .addSupportedProfile(ProfileValidation.MONITOR_PROFILE)
                .setDocumentation("GET Device/{clinicId" + ID_DELIMITER + "patientId"
                        + ID_DELIMITER + "deviceId} returns the CIED implanted in the patient’s body, referenced by the "
                        + "Observation cied-device extension. The optional _profile read parameter "
                        + "defaults to " + ProfileValidation.DEVICE_PROFILE + " and accepts only that canonical URL. "
                        + "The mapping covers manufacturer, serial number, model number and device type; "
                        + "UDI is not mapped.");
        device.addInteraction().setCode(TypeRestfulInteraction.READ);

        var observation = server.addResource()
                .setType("Observation")
                .addSupportedProfile(ProfileValidation.CONNECTIVITY_PROFILE)
                .setDocumentation("GET Observation?clinicId={clinicId} searches CIED connectivity status "
                        + "observations for a clinic. GET Observation?clinicId={clinicId}&patientId={patientId} "
                        + "restricts the search to a patient in that clinic. Both return a searchset Bundle. "
                        + "clinicId and patientId are application-specific identifiers, not FHIR references. "
                        + "Matching Observations use search.mode=match; related resources use search.mode=include. "
                        + "Resources rejected during mapping or profile validation are reported as OperationOutcome "
                        + "entries with search.mode=outcome; Bundle.total counts matching Observations only. "
                        + "Optional _include values may be combined by repeating the parameter: "
                        + ObservationService.INCLUDE_SUBJECT + " includes the Patient; "
                        + ObservationService.INCLUDE_DEVICE + " includes the bedside monitor (transmitter) referenced "
                        + "by Observation.device, conforming to the CIED Monitor profile; "
                        + ObservationService.INCLUDE_CIED_DEVICE + " includes the CIED implanted in the patient’s body, "
                        + "referenced by the cied-device extension and conforming to the CIED Device profile. "
                        + "These are distinct devices represented by the same FHIR resource type, Device. "
                        + "GET Device/{id} retrieves the implanted CIED. The bedside monitor communicates with "
                        + "the implanted CIED and is available as an included resource in the Observation search Bundle. "
                        + "The monitor mapping supplies model and serial number only; manufacturer, type and UDI "
                        + "are not mapped. Mapped Observation components are connectivity status, connectivity modifier, "
                        + "last CIED connectivity time, last monitor connectivity time and status guidance; "
                        + "other profile components are not mapped. "
                        + "Other _include values return HTTP 400. "
                        + "No pagination or additional search filters are supported.");
        observation.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
        observation.addSearchParam().setName("clinicId").setType(SearchParamType.STRING)
                .setDocumentation("Required. Clinic identifier defining the scope of both Observation searches.");
        observation.addSearchParam().setName("patientId").setType(SearchParamType.STRING)
                .setDocumentation("Optional. Patient identifier within the required clinic; "
                        + "when present, selects the patient-specific connectivity search.");
        observation.addSearchInclude(ObservationService.INCLUDE_SUBJECT);
        observation.addSearchInclude(ObservationService.INCLUDE_DEVICE);
        observation.addSearchInclude(ObservationService.INCLUDE_CIED_DEVICE);

        var narrative = CapabilityStatementDto.render(statement);
        statement.setText(new Narrative(NarrativeStatus.GENERATED, narrative));
        metadataJson = context.newJsonParser().encodeResourceToString(statement);
        metadataHtml = """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>%s</title>
                    <style>
                        body { font: 1rem/1.5 system-ui, sans-serif; margin: 2rem auto; max-width: 68rem; padding: 0 1rem; }
                        .table-scroll { overflow-x: auto; }
                        table { width: 100%%; min-width: 36rem; border-collapse: collapse; }
                        th, td { padding: .65rem; border: 1px solid #888; text-align: left; vertical-align: top; }
                        th:first-child { width: 9rem; white-space: nowrap; }
                        th:nth-child(2) { width: 10rem; white-space: nowrap; }
                        p, li { overflow-wrap: anywhere; }
                        td code { white-space: nowrap; }
                        h3 { margin-top: 2rem; }
                        td ul { margin: 0; padding-inline-start: 1.2em; }
                    </style>
                </head>
                <body>
                    <main>%s</main>
                    <p><a href="?_format=json">View FHIR JSON</a></p>
                </body>
                </html>
                """.formatted(statement.getTitle(), new XhtmlComposer(XhtmlComposer.XML).compose(narrative));
    }

    @GetMapping("/fhir/metadata")
    public ResponseEntity<String> metadata(@RequestParam(name = "_format", required = false) String format,
                                           @RequestHeader HttpHeaders headers) {
        MediaType mediaType = selectMediaType(format, headers);
        if (mediaType == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).varyBy(HttpHeaders.ACCEPT).build();
        }
        return ResponseEntity.ok().varyBy(HttpHeaders.ACCEPT).contentType(mediaType)
                .body(mediaType.equals(HTML_UTF8) ? metadataHtml : metadataJson);
    }

    private static MediaType selectMediaType(String format, HttpHeaders headers) {
        if (format != null) {
            return switch (format.trim().toLowerCase(Locale.ROOT)) {
                case "html", "text/html" -> HTML_UTF8;
                case "json", "application/json", "application/fhir+json" -> FHIR_JSON;
                default -> null;
            };
        }

        List<MediaType> accepted;
        try {
            accepted = headers.getAccept();
        } catch (InvalidMediaTypeException e) {
            return null;
        }
        if (accepted.isEmpty()) {
            return FHIR_JSON;
        }
        double jsonQuality = qualityFor(FHIR_JSON, accepted);
        double htmlQuality = qualityFor(HTML_UTF8, accepted);
        if (htmlQuality > jsonQuality) {
            return HTML_UTF8;
        }
        return jsonQuality > 0 ? FHIR_JSON : null;
    }

    private static double qualityFor(MediaType representation, List<MediaType> accepted) {
        // A specific media range takes precedence over a wildcard, even when it has q=0.
        return accepted.stream().filter(range -> range.includes(representation))
                .max(Comparator.comparingInt((MediaType range) -> range.isWildcardType() ? 0
                        : range.isWildcardSubtype() ? 1 : 2).thenComparingDouble(MediaType::getQualityValue))
                .map(MediaType::getQualityValue).orElse(0.0);
    }
}
