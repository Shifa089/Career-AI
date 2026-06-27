package com.careerai.interview;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the CareerAI layered architecture for interview-service.
 */
@AnalyzeClasses(packages = "com.careerai.interview", importOptions = com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layeredArchitecture = Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy("..controller..")
            .layer("Service").definedBy("..service..")
            .layer("Repository").definedBy("..repository..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule repositoriesMustNotDependOnServices = noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAPackage("..service..");
}
