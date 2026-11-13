package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r5.model.CapabilityStatement.TypeRestfulInteraction;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.Enumerations.CapabilityStatementKind;
import org.hl7.fhir.r5.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Narrative;
import org.hl7.fhir.r5.model.Narrative.NarrativeStatus;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestController
public class MetadataController {
    private static final MediaType FHIR_JSON = MediaType.valueOf("application/fhir+json");
    private static final MediaType HTML_UTF8 = MediaType.valueOf("text/html;charset=UTF-8");

    private final String metadataJson;
    private final String metadataHtml;

    public MetadataController(FhirContext context, @Value("${fhir.public-base-url:}") String publicBaseUrl) {
        CapabilityStatement statement = new CapabilityStatement();
        statement.setName("MyCiedProxy");
        statement.setStatus(PublicationStatus.DRAFT);
        statement.setDateElement(new DateTimeType("2026-11-12"));
        statement.setKind(CapabilityStatementKind.INSTANCE);
        statement.getImplementation().setDescription("My CIED FHIR proxy");
        if (!publicBaseUrl.isBlank()) {
            statement.getImplementation().setUrl(publicBaseUrl);
        }
        statement.setFhirVersion(FHIRVersion._5_0_0);
        statement.addFormat("json");
        statement.addImplementationGuide(ProfileValidation.CARDX_CIED_GUIDE);

        var server = statement.addRest()
                .setMode(RestfulCapabilityMode.SERVER)
                .setDocumentation("My patient and CIED API draft contract.");

        var patient = server.addResource()
                .setType("Patient")
                .addSupportedProfile(ProfileValidation.PATIENT_PROFILE)
                .setDocumentation("Planned: read a patient by ID and list patients within a single My clinic.");
        patient.addInteraction().setCode(TypeRestfulInteraction.READ);
        patient.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);

        server.addResource()
                .setType("Device")
                .addSupportedProfile(ProfileValidation.DEVICE_PROFILE)
                .addSupportedProfile(ProfileValidation.MONITOR_PROFILE)
                .setDocumentation("Implanted devices and monitors are mapped from device data returned for a patient.");

        server.addResource()
                .setType("Observation")
                .addSupportedProfile(ProfileValidation.CONNECTIVITY_PROFILE)
                .setDocumentation("CIED connectivity observations are planned when patient device data provides the required connectivity details.");

        var narrative = CapabilityStatementNarrative.render(statement);
        statement.setText(new Narrative(NarrativeStatus.GENERATED, narrative));
        metadataJson = context.newJsonParser().encodeResourceToString(statement);
        metadataHtml = """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>FHIR server capabilities</title>
                    <style>
                        body { font: 1rem/1.5 system-ui, sans-serif; margin: 2rem auto; max-width: 68rem; padding: 0 1rem; }
                        table { width: 100%%; border-collapse: collapse; }
                        th, td { padding: .65rem; border: 1px solid #888; text-align: left; vertical-align: top; }
                        code { overflow-wrap: anywhere; }
                        td ul { margin: 0; padding-inline-start: 1.2em; }
                    </style>
                </head>
                <body>
                    <main>%s</main>
                    <p><a href="?_format=json">View FHIR JSON</a></p>
                </body>
                </html>
                """.formatted(new XhtmlComposer(XhtmlComposer.XML).compose(narrative));
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
