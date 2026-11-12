# fhir-dummy

Java 21 / Spring Boot application with a HAPI FHIR R4 client.

## Test coverage

Run `./mvnw clean verify` with JDK 21. JaCoCo writes the HTML report to
`target/site/jacoco/index.html` and the XML report to `target/site/jacoco/jacoco.xml`.

## SonarQube

Create a project with the key `fhir-dummy` on your SonarQube server. Set
`SONAR_HOST_URL` to the server URL and supply a token with Execute Analysis
permission through the `SONAR_TOKEN` environment variable.

```sh
./mvnw clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
```

The scanner imports the JaCoCo XML report. Open the project's **Measures** page
in SonarQube to view **Coverage**.
