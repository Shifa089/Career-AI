package com.careerai.auth;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the CareerAI layered architecture for auth-service.
 *
 * <p>auth-service is still a scaffold (only {@code AuthServiceApplication}; the
 * controller/service/repository packages are empty). ArchUnit fails a rule that matches no classes
 * by default, so the rules are made empty-tolerant here: {@code withOptionalLayers(true)} allows the
 * (currently empty) layers, and {@code allowEmptyShould(true)} allows the no-class rules. Once the
 * service has real classes the rules enforce exactly as in the other services — nothing that does
 * exist is exempted.</p>
 */
@AnalyzeClasses(packages = "com.careerai.auth", importOptions = com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layeredArchitecture = Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Controller").definedBy("..controller..")
            .layer("Service").definedBy("..service..")
            .layer("Repository").definedBy("..repository..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule controllersAreSuffixed = noClasses()
            .that().resideInAPackage("..controller..")
            .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().haveSimpleNameNotEndingWith("ServiceImpl")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repositoriesMustNotDependOnServices = noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAPackage("..service..")
            .allowEmptyShould(true);
}
