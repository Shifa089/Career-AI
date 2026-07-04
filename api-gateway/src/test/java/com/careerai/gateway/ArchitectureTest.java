package com.careerai.gateway;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture rules for the gateway. A Spring Cloud Gateway is routing/filter configuration rather
 * than a layered controller/service/repository app, so the rules here guard the gateway-specific
 * package conventions and are made empty-tolerant ({@code allowEmptyShould(true)}) where a rule may
 * legitimately match no classes.
 */
@AnalyzeClasses(packages = "com.careerai.gateway",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule globalFiltersLiveInFilterPackage = classes()
            .that().implement(org.springframework.cloud.gateway.filter.GlobalFilter.class)
            .should().resideInAPackage("..filter..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllersAreSuffixed = classes()
            .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().haveSimpleNameEndingWith("Controller")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule configClassesResideInConfigPackage = classes()
            .that().haveSimpleNameEndingWith("Config")
            .should().resideInAPackage("..config..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule filtersMustNotDependOnControllers = noClasses()
            .that().resideInAPackage("..filter..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .allowEmptyShould(true);
}
