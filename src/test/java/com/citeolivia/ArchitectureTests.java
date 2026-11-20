package com.citeolivia;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.data.repository.Repository;

@AnalyzeClasses(packages = "com.citeolivia", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

    @ArchTest
    static final ArchRule controllersDoNotAccessRepositories = noClasses()
            .that()
            .resideInAPackage("..web..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(Repository.class)
            .because("controllers go through services");

    @ArchTest
    static final ArchRule passwordHashingOnlyInSecurityPackage = noClasses()
            .that()
            .resideOutsideOfPackage("..security..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.springframework.security.crypto..")
            .because("password hashing is encapsulated in the security package (PasswordHasher)");

    @ArchTest
    static final ArchRule noJpa = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "org.hibernate.orm..", "org.springframework.data.jpa..")
            .because("persistence uses Spring Data JDBC only");

    @ArchTest
    static final ArchRule layers = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Web")
            .definedBy("..web..")
            .layer("Domain")
            .definedBy("..user..", "..library..")
            .layer("Security")
            .definedBy("..security..")
            .whereLayer("Web")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Web")
            .whereLayer("Security")
            .mayOnlyBeAccessedByLayers("Web", "Domain");
}
