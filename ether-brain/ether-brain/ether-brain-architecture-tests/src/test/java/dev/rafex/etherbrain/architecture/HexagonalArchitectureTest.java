package dev.rafex.etherbrain.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

public class HexagonalArchitectureTest {

    private static final ArchRule CORE_DOES_NOT_DEPEND_ON_INFRASTRUCTURE =
            noClasses()
                    .that().resideInAPackage("..core..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..infra..", "..tools.local..", "..cli..", "..bootstrap..");

    @Test
    void coreDoesNotDependOnInfrastructure() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("dev.rafex.etherbrain");

        CORE_DOES_NOT_DEPEND_ON_INFRASTRUCTURE.check(classes);
    }
}
