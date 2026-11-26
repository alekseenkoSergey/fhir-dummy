package org.example.fhirdummy.component.mapper;

import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Element;
import org.hl7.fhir.r5.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceMapperTest {
    private static final String VALUE_SET = "http://hl7.org/fhir/uv/cardx-cied/ValueSet/MDCIDCDeviceTypes";
    private static final String ABSENT_REASON = "http://hl7.org/fhir/StructureDefinition/data-absent-reason";
    @Mock private NpmPackageValidationSupport validationSupport;
    private final JsonMapper json = new JsonMapper();

    @Test
    void failsWhenDeviceTypesValueSetIsMissing() {
        assertEquals("Missing CIED device types ValueSet in the IG package",
                assertThrows(IllegalStateException.class, () -> new DeviceMapper(validationSupport)).getMessage());
        verify(validationSupport).fetchValueSet(VALUE_SET);
    }

    @Test
    void mapsCompleteDeviceUsingCodingFromMatchingValueSetInclude() {
        var mapper = mapperWithTypes();
        var source = json.readTree("""
                {"deviceDescription":" Manufacturer ","deviceSerialNum":" serial ",
                 "deviceModelNum":" model ","deviceType":" Defibrillator "}
                """);

        var device = mapper.toFhir(source, "clinic", "patient", "device");

        assertEquals("clinic-patient-device", device.getIdElement().getIdPart());
        assertEquals(1, device.getIdentifier().size());
        assertEquals("clinic-patient-device", device.getIdentifierFirstRep().getValue());
        assertEquals("Manufacturer", device.getManufacturer());
        assertEquals("serial", device.getSerialNumber());
        assertEquals("model", device.getModelNumber());
        assertFalse(device.getManufacturerElement().hasExtension());
        assertFalse(device.getSerialNumberElement().hasExtension());
        assertFalse(device.getModelNumberElement().hasExtension());
        assertEquals(1, device.getType().size());
        var type = device.getTypeFirstRep();
        assertFalse(type.hasExtension());
        assertEquals(1, type.getCoding().size());
        var coding = type.getCodingFirstRep();
        assertEquals("urn:second-system", coding.getSystem());
        assertEquals("defib", coding.getCode());
        assertEquals("Defibrillator", coding.getDisplay());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}",
            "{\"deviceDescription\":null,\"deviceSerialNum\":null,\"deviceModelNum\":null,\"deviceType\":null}",
            "{\"deviceDescription\":\" \",\"deviceSerialNum\":\" \",\"deviceModelNum\":\" \",\"deviceType\":\" \"}"})
    void representsMissingNullAndBlankFieldsWithUnknownExtensions(String input) {
        var device = mapperWithTypes().toFhir(json.readTree(input), "clinic", "patient", "device");

        assertFalse(device.getManufacturerElement().hasValue());
        assertFalse(device.getSerialNumberElement().hasValue());
        assertFalse(device.getModelNumberElement().hasValue());
        assertFalse(device.getTypeFirstRep().hasCoding());
        assertUnknown(device.getManufacturerElement());
        assertUnknown(device.getSerialNumberElement());
        assertUnknown(device.getModelNumberElement());
        assertUnknown(device.getTypeFirstRep());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"null", "[]", "42", "true", "\"device\""})
    void rejectsNonObjectSources(String input) {
        var mapper = mapperWithTypes();
        var source = input == null ? null : json.readTree(input);

        assertEquals("Expected a device object", assertThrows(IllegalArgumentException.class,
                () -> mapper.toFhir(source, "clinic", "patient", "device")).getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Unsupported", "pacemaker", "MDC_IDC_ENUM_DEV_TYPE_Other"})
    void mapsUnknownAndExplicitOtherDeviceTypesToOther(String type) {
        var mapper = mapperWithTypes();
        var source = json.createObjectNode().put("deviceType", type);

        var device = mapper.toFhir(source, "clinic", "patient", "device");

        assertEquals(1, device.getType().size());
        var mappedType = device.getTypeFirstRep();
        assertFalse(mappedType.hasExtension());
        assertEquals(1, mappedType.getCoding().size());
        var coding = mappedType.getCodingFirstRep();
        assertEquals("urn:iso:std:iso:11073:10101", coding.getSystem());
        assertEquals("753670", coding.getCode());
        assertEquals("MDC_IDC_ENUM_DEV_TYPE_Other", coding.getDisplay());
    }

    @ParameterizedTest
    @ValueSource(strings = {"deviceDescription", "deviceSerialNum", "deviceModelNum", "deviceType"})
    void rejectsNonTextFields(String field) {
        var mapper = mapperWithTypes();
        var source = json.createObjectNode().put(field, 42);

        assertEquals("Expected text for field " + field, assertThrows(IllegalArgumentException.class,
                () -> mapper.toFhir(source, "clinic", "patient", "device")).getMessage());
    }

    @ParameterizedTest
    @CsvSource({"Pacemaker,pace,urn:first-system", "Unsupported,753670,urn:iso:std:iso:11073:10101"})
    void copiesCodingSoMutatingOneDeviceDoesNotAffectOthers(String type, String code, String system) {
        var mapper = mapperWithTypes();
        var source = json.createObjectNode().put("deviceType", type);
        var first = mapper.toFhir(source, "clinic", "patient", "first");
        first.getTypeFirstRep().getCodingFirstRep().setCode("changed");

        var second = mapper.toFhir(source, "clinic", "patient", "second");

        assertEquals(code, second.getTypeFirstRep().getCodingFirstRep().getCode());
        assertEquals(system, second.getTypeFirstRep().getCodingFirstRep().getSystem());
    }

    @Test
    void supportsAbsentTypeEvenWhenValueSetHasNoConcepts() {
        when(validationSupport.fetchValueSet(VALUE_SET)).thenReturn(new ValueSet());
        var mapper = new DeviceMapper(validationSupport);

        assertUnknown(mapper.toFhir(json.readTree("{}"), "clinic", "patient", "device").getTypeFirstRep());
        assertEquals("Missing MDC_IDC_ENUM_DEV_TYPE_Other in the CIED device types ValueSet",
                assertThrows(IllegalStateException.class, () -> mapper.toFhir(
                        json.createObjectNode().put("deviceType", "Pacemaker"),
                        "clinic", "patient", "device")).getMessage());
    }

    private DeviceMapper mapperWithTypes() {
        var valueSet = new ValueSet();
        var first = valueSet.getCompose().addInclude().setSystem("urn:first-system");
        first.addConcept().setCode("pace").setDisplay("Pacemaker");
        first.addConcept().setCode("other").setDisplay("Other");
        valueSet.getCompose().addInclude().setSystem("urn:second-system")
                .addConcept().setCode("defib").setDisplay("Defibrillator");
        valueSet.getCompose().addInclude().setSystem("urn:iso:std:iso:11073:10101")
                .addConcept().setCode("753670").setDisplay("MDC_IDC_ENUM_DEV_TYPE_Other");
        when(validationSupport.fetchValueSet(VALUE_SET)).thenReturn(valueSet);
        return new DeviceMapper(validationSupport);
    }

    private void assertUnknown(Element element) {
        assertEquals(1, element.getExtension().size());
        var extension = element.getExtensionByUrl(ABSENT_REASON);
        assertNotNull(extension);
        assertEquals("unknown", assertInstanceOf(CodeType.class, extension.getValue()).getValue());
    }
}
