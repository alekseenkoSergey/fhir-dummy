package org.example.fhirdummy.controller;

import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

final class CapabilityStatementNarrative {
    private CapabilityStatementNarrative() {}

    static XhtmlNode render(CapabilityStatement statement) {
        var div = new XhtmlNode(NodeType.Element, "div").setAttribute("xmlns", XhtmlComposer.XHTML_NS);
        div.addTag("h1").addText(statement.getName());
        div.addTag("p").addText(statement.getImplementation().getDescription());
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
            div.addTag("h2").addText("REST capabilities");
            if (rest.hasDocumentation()) {
                div.addTag("p").addText(rest.getDocumentation());
            }
            var table = div.addTag("table");
            var header = table.addTag("thead").addTag("tr");
            for (String heading : new String[]{"Resource", "Profiles", "Interactions", "Details"}) {
                header.addTag("th").setAttribute("scope", "col").addText(heading);
            }
            var body = table.addTag("tbody");
            for (var resource : rest.getResource()) {
                var row = body.addTag("tr");
                row.addTag("th").setAttribute("scope", "row").addText(resource.getType());
                var profiles = row.td().addTag("ul");
                for (var profile : resource.getSupportedProfile()) {
                    profiles.addTag("li").addTag("code").addText(profile.getValue());
                }
                var interactions = row.td();
                if (resource.hasInteraction()) {
                    var list = interactions.addTag("ul");
                    for (var interaction : resource.getInteraction()) {
                        list.addTag("li").addTag("code").addText(interaction.getCode().toCode());
                    }
                } else {
                    interactions.addText("No standalone interactions declared");
                }
                row.td().addText(resource.getDocumentation());
            }
        }
        return div;
    }
}
