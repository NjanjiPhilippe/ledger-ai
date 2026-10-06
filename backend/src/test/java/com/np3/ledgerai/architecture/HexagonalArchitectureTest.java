package com.np3.ledgerai.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Executable version of docs/adr/0001-hexagonal-conventions.md.
 * If one of these fails, a dependency crossed a boundary the architecture forbids.
 */
@AnalyzeClasses(packages = "com.np3.ledgerai", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "com.np3.ledgerai.domain..";
    private static final String APPLICATION = "com.np3.ledgerai.application..";
    private static final String INFRASTRUCTURE = "com.np3.ledgerai.infrastructure..";
    private static final String WEB = "com.np3.ledgerai.web..";

    @ArchTest
    static final ArchRule layersOnlyDependInward = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy(DOMAIN)
            .layer("Application").definedBy(APPLICATION)
            .layer("Infrastructure").definedBy(INFRASTRUCTURE)
            .layer("Web").definedBy(WEB)
            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Web", "Infrastructure");

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses().that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "jakarta.validation..",
                    "com.fasterxml..", "tools.jackson..", "lombok..")
            .because("the domain must stay pure Java");

    @ArchTest
    static final ArchRule applicationDoesNotKnowThePersistenceTechnology = noClasses().that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..", "org.springframework.data..", "org.springframework.jdbc..")
            .because("persistence is reached through domain ports only");

    @ArchTest
    static final ArchRule portsAreInterfaces = classes().that().resideInAPackage("com.np3.ledgerai.domain.port")
            .and().haveSimpleNameEndingWith("Repository")
            .or().haveSimpleNameEndingWith("Port")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule applicationHasNoInterfaces = classes().that().resideInAPackage(APPLICATION)
            .should().notBeInterfaces()
            .because("use cases are concrete classes; interfaces are reserved for outbound ports in the domain");

    @ArchTest
    static final ArchRule useCasesAreNamedUseCase = classes().that().resideInAPackage("..application..useCase..")
            .should().haveSimpleNameEndingWith("UseCase");

    @ArchTest
    static final ArchRule queriesAreNamedQuery = classes().that().resideInAPackage("..application..query..")
            .should().haveSimpleNameEndingWith("Query");
}
