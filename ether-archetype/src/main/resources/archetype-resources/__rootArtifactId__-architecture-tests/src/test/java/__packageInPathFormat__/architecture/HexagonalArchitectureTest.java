package ${package}.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit tests that enforce hexagonal architecture boundaries.
 * <p>
 * Dependency rules:
 * <pre>
 *   Transport Adapters ──┐
 *                        ▼
 *   Bootstrap ──────► Core ◄── Infra Adapters
 *                        ▲
 *                      Ports (interfaces)
 *                        ▲
 *   Common (no outbound dependencies)
 * </pre>
 * </p>
 */
@AnalyzeClasses(packages = "${package}")
public class HexagonalArchitectureTest {

    private static final String[] ADAPTER_PACKAGES = {
        "${package}.repository.impl..",
        "${package}.db..",
        "${package}.bootstrap..",
        "${package}.handlers..",
        "${package}.server..",
        "${package}.http..",
        "${package}.json..",
        "${package}.dtos..",
        "${package}.transport.."
    };

    @ArchTest
    static final ArchRule ports_must_not_depend_on_core_or_adapters = noClasses()
        .that().resideInAPackage("${package}.repository")
        .should().dependOnClassesThat().resideInAnyPackage(
            "${package}.services..",
            "${package}.models..",
            "${package}.repository.impl..",
            "${package}.db..",
            "${package}.bootstrap..",
            "${package}.handlers..",
            "${package}.server..",
            "${package}.http..",
            "${package}.json..",
            "${package}.dtos..",
            "${package}.transport.."
        );

    @ArchTest
    static final ArchRule common_must_not_depend_on_core_or_adapters = noClasses()
        .that().resideInAnyPackage(
            "${package}.config..",
            "${package}.errors..",
            "${package}.logging.."
        )
        .should().dependOnClassesThat().resideInAnyPackage(
            "${package}.services..",
            "${package}.models..",
            "${package}.repository..",
            "${package}.repository.impl..",
            "${package}.db..",
            "${package}.bootstrap..",
            "${package}.handlers..",
            "${package}.server..",
            "${package}.http..",
            "${package}.json..",
            "${package}.dtos..",
            "${package}.transport.."
        );

    @ArchTest
    static final ArchRule core_must_not_depend_on_adapters = noClasses()
        .that().resideInAPackage("${package}.services..")
        .should().dependOnClassesThat().resideInAnyPackage(ADAPTER_PACKAGES);

    @ArchTest
    static final ArchRule infra_must_not_depend_on_transport_or_core_services = noClasses()
        .that().resideInAnyPackage("${package}.repository.impl..", "${package}.db..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "${package}.services..",
            "${package}.models..",
            "${package}.bootstrap..",
            "${package}.handlers..",
            "${package}.server..",
            "${package}.http..",
            "${package}.json..",
            "${package}.dtos..",
            "${package}.transport.."
        );

    @ArchTest
    static final ArchRule transport_must_not_depend_on_infra_details = noClasses()
        .that().resideInAnyPackage(
            "${package}.handlers..",
            "${package}.server..",
            "${package}.http..",
            "${package}.json..",
            "${package}.dtos..",
            "${package}.transport.."
        )
        .should().dependOnClassesThat().resideInAnyPackage(
            "${package}.repository.impl..",
            "${package}.db.."
        );

    @ArchTest
    static final ArchRule bootstrap_must_not_depend_on_transport = noClasses()
        .that().resideInAPackage("${package}.bootstrap..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "${package}.handlers..",
            "${package}.server..",
            "${package}.http..",
            "${package}.json..",
            "${package}.dtos..",
            "${package}.transport.."
        );
}
