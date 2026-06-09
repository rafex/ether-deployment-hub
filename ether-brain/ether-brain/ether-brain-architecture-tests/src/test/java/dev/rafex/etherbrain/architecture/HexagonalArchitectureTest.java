package dev.rafex.etherbrain.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.Test;

class HexagonalArchitectureTest {

    private static final ArchRule CORE_DOES_NOT_DEPEND_ON_INFRASTRUCTURE =
            noClasses()
                    .that().resideInAPackage("dev.rafex.etherbrain.core..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "dev.rafex.etherbrain.infra..",
                            "dev.rafex.etherbrain.tools..",
                            "dev.rafex.etherbrain.cli..",
                            "dev.rafex.etherbrain.http..",
                            "dev.rafex.etherbrain.mqtt..",
                            "dev.rafex.etherbrain.bootstrap..");

    private static final ArchRule PORTS_DO_NOT_DEPEND_ON_INTERNALS =
            noClasses()
                    .that().resideInAPackage("dev.rafex.etherbrain.ports..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "dev.rafex.etherbrain.core..",
                            "dev.rafex.etherbrain.infra..",
                            "dev.rafex.etherbrain.tools..",
                            "dev.rafex.etherbrain.bootstrap..",
                            "dev.rafex.etherbrain.cli..",
                            "dev.rafex.etherbrain.http..",
                            "dev.rafex.etherbrain.mqtt..");

    private static final ArchRule BOOTSTRAP_DOES_NOT_DEPEND_ON_TRANSPORTS =
            noClasses()
                    .that().resideInAPackage("dev.rafex.etherbrain.bootstrap..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "dev.rafex.etherbrain.cli..",
                            "dev.rafex.etherbrain.http..",
                            "dev.rafex.etherbrain.mqtt..");

    private static final ArchRule HTTP_TRANSPORT_DOES_NOT_DEPEND_ON_OTHER_TRANSPORTS =
            noClasses()
                    .that().resideInAPackage("dev.rafex.etherbrain.http..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "dev.rafex.etherbrain.cli..",
                            "dev.rafex.etherbrain.mqtt..");

    private static final ArchRule MQTT_TRANSPORT_DOES_NOT_DEPEND_ON_OTHER_TRANSPORTS =
            noClasses()
                    .that().resideInAPackage("dev.rafex.etherbrain.mqtt..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "dev.rafex.etherbrain.cli..",
                            "dev.rafex.etherbrain.http..");

    private static final ArchRule CLI_TRANSPORT_DOES_NOT_DEPEND_ON_OTHER_TRANSPORTS =
            noClasses()
                    .that().resideInAPackage("dev.rafex.etherbrain.cli..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "dev.rafex.etherbrain.http..",
                            "dev.rafex.etherbrain.mqtt..");

    @Test
    void enforcesHexagonalArchitecture() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("dev.rafex.etherbrain");

        List.of(
                        CORE_DOES_NOT_DEPEND_ON_INFRASTRUCTURE,
                        PORTS_DO_NOT_DEPEND_ON_INTERNALS,
                        BOOTSTRAP_DOES_NOT_DEPEND_ON_TRANSPORTS,
                        HTTP_TRANSPORT_DOES_NOT_DEPEND_ON_OTHER_TRANSPORTS,
                        MQTT_TRANSPORT_DOES_NOT_DEPEND_ON_OTHER_TRANSPORTS,
                        CLI_TRANSPORT_DOES_NOT_DEPEND_ON_OTHER_TRANSPORTS)
                .forEach(rule -> rule.check(classes));
    }
}
