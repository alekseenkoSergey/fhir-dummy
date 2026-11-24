package org.example.fhirdummy.dtos;

import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

/**
 * Renders a FHIR CapabilityStatement as a human-readable XHTML narrative.
 * It summarizes server details, implementation guides, supported resource profiles, and REST interactions
 * for use in both the resource narrative and the HTML metadata page.
 */
public final class CapabilityStatementDto {
    private CapabilityStatementDto() {}

    public static XhtmlNode render(CapabilityStatement statement) {
        var div = new XhtmlNode(NodeType.Element, "div").setAttribute("xmlns", XhtmlComposer.XHTML_NS);
        div.addTag("h1").addText(statement.getTitle());
        div.addTag("p").addText(statement.getImplementation().getDescription());
        div.addTag("p").addText(statement.getDescription());
        div.addTag("p").addText("Kind: " + statement.getKind().toCode()
                + " · Resource formats: " + statement.getFormat().stream()
                .map(format -> format.getValue()).collect(java.util.stream.Collectors.joining(", ")));
        div.addTag("p").addText("FHIR " + statement.getFhirVersion().toCode()
                + " · Status: " + statement.getStatus().toCode() + " · Updated: " + statement.getDateElement().asStringValue());
        if (statement.getImplementation().hasUrl()) {
            var baseUrl = div.addTag("p");
            baseUrl.addText("FHIR base URL: ");
            baseUrl.addTag("code").addText(statement.getImplementation().getUrl());
        }
        for (var guide : statement.getImplementationGuide()) {
            var guideReference = div.addTag("p");
            guideReference.addText("Implementation guide: ");
            guideReference.addTag("code").addText(guide.getValue());
        }

        for (var rest : statement.getRest()) {
            div.addTag("h2").addText("REST capabilities (" + rest.getMode().toCode() + ")");
            if (rest.hasDocumentation()) {
                div.addTag("p").addText(rest.getDocumentation());
            }
            if (rest.hasSecurity() && rest.getSecurity().hasDescription()) {
                div.addTag("p").addText("Security: " + rest.getSecurity().getDescription());
            }
            var table = div.addTag("div").setAttribute("class", "table-scroll").addTag("table");
            var header = table.addTag("thead").addTag("tr");
            for (String heading : new String[]{"Resource", "Interactions", "Profiles"}) {
                header.addTag("th").setAttribute("scope", "col").addText(heading);
            }
            var body = table.addTag("tbody");
            for (var resource : rest.getResource()) {
                var row = body.addTag("tr");
                row.addTag("th").setAttribute("scope", "row").addText(resource.getType());
                var interactions = row.td();
                if (resource.hasInteraction()) {
                    var list = interactions.addTag("ul");
                    for (var interaction : resource.getInteraction()) {
                        list.addTag("li").addTag("code").addText(interaction.getCode().toCode());
                    }
                } else {
                    interactions.addText("No standalone interactions declared");
                }
                var profiles = row.td().addTag("ul");
                for (var profile : resource.getSupportedProfile()) {
                    String canonical = profile.getValue();
                    profiles.addTag("li").addTag("a").setAttribute("href", canonical)
                            .addText(canonical.substring(canonical.lastIndexOf('/') + 1));
                }
            }
            for (var resource : rest.getResource()) {
                var details = div.addTag("div");
                details.addTag("h3").addText(resource.getType());
                details.addTag("p").addText(resource.getDocumentation());
                if (resource.hasSearchParam()) {
                    details.addTag("p").addText("Search parameters:");
                    var parameters = details.addTag("ul");
                    for (var parameter : resource.getSearchParam()) {
                        var item = parameters.addTag("li");
                        item.addTag("code").addText(parameter.getName());
                        item.addText(" (" + parameter.getType().toCode() + "): " + parameter.getDocumentation());
                    }
                }
                if (resource.hasSearchInclude()) {
                    details.addTag("p").addText("Supported _include values:");
                    var includes = details.addTag("ul");
                    for (var include : resource.getSearchInclude()) {
                        includes.addTag("li").addTag("code").addText(include.getValue());
                    }
                }
            }
        }
        return div;
    }
}
