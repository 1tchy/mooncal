import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Layering rules that a reviewer would otherwise have to spot by hand.
 */
class ArchitectureTest {

	private final JavaClasses classes = new ClassFileImporter().importPackages("controllers", "logics", "models");

	@Test
	void logicsDoesNotDependOnControllers() {
		noClasses().that().resideInAPackage("logics..")
				.should().dependOnClassesThat().resideInAPackage("controllers..")
				.because("controllers call logics, never the other way round")
				.check(classes);
	}
}
