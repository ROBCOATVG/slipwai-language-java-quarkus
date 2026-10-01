"""What the Quarkus framework decides about the project around its service: the build output git ignores, the gate's
test harness, and why `make mutation` is a documented placeholder rather than PIT.

Moved from the `java` family's `java_project.py` (S05's answers), because they are this framework's and not the
family's: a framework author changes them in this package."""
from __future__ import annotations

from typing import Any

from slipwai import registry as protocol

# What `make mutation` does on the Quarkus backend, and why it is not PIT already wired up.
#
# Per backend rather than per family, and the Spring sibling is the reason: PIT *is* wired up there, so a
# note shared across the family would tell one of the two something flatly untrue about its own build.
#
# PIT is the ecosystem's mutation tester and pitest-junit5-plugin 1.2.3 is the first version claiming
# Quarkus support — so the tool is not in doubt. What is in doubt is running it over a `@QuarkusTest`:
# pitest issue #1287 reports tests that pass standalone timing out on every mutation under PIT, on the
# configuration that behaves fine for Spring Boot — which is the one `java-spring` in this same factory
# ships wired up. That is exactly the failure this factory could not
# catch, because `make mutation` is run by no gate at either level (docs/backend-obligations.md section 2),
# so a wired-up PIT would have been committed green and stayed green.
#
# The mitigation is real and is written down below rather than guessed at later: mutate the domain, which
# is framework-free by construction here, with its plain JUnit tests — and keep `@QuarkusTest` and `*IT`
# out of PIT's reach. That is a decision about which classes this product considers worth mutating, which
# is why it is a decision the project makes rather than one the factory pins.
JAVA_QUARKUS_MUTATION_NOTE = """\
# Not wired up, on purpose, and the reason is a support constraint rather than a missing dependency.
#
# PIT is the JVM's mutation tester, and pitest-junit5-plugin 1.2.3 is the first version that claims
# Quarkus support. But pitest issue #1287 reports `@QuarkusTest` classes that pass standalone timing out
# on every mutation once PIT runs them — the same configuration works for Spring Boot, which is why the
# `java-spring` backend here ships `make mutation` wired up and this one does not. Nothing in this
# repository's gates runs this target, so a broken configuration here would never fail a build; it would
# just quietly never have worked.
#
# So configure it deliberately, and narrowly:
#
#   1. targetClasses — the domain packages only. They are framework-free by construction, which is the
#      whole reason the hexagon puts them there, and they are where a surviving mutant means something.
#   2. targetTests — the plain JUnit tests over those packages. Exclude `@QuarkusTest` and every `*IT`.
#   3. pitest-junit5-plugin as a dependency of the pitest-maven *plugin*, not of the project. Declared as
#      a project dependency, PIT reports "0 tests found" and does nothing, which is the most common way a
#      first PIT setup silently passes.
#
# Then run it, look at the survivors, and only afterwards let anything depend on the score.
"""

JAVA_QUARKUS_MUTATION_PLACEHOLDER = (
    "@echo 'Configure PIT for the domain packages only — see the note above this target — then run it.'; "
    "exit 2"
)


QUARKUS: dict[protocol.Member[Any], object] = {
    # `target/` is every artifact Maven writes — classes, the Quarkus build output, the analysers'
    # reports. `.flattened-pom.xml` is what the Quarkus build leaves behind when it resolves the
    # platform BOM, and it is derived from the pom rather than edited beside it.
    protocol.GITIGNORE: "target/\n.flattened-pom.xml\n",
    protocol.GATE_DESCRIPTION: (
        "Checkstyle, PMD and SpotBugs for lint; `javac` with Error Prone and NullAway for the type check; "
        "JUnit 5 with Quarkus's own test harness, coverage through the `quarkus-jacoco` extension"
    ),
    protocol.MUTATION_NOTE: JAVA_QUARKUS_MUTATION_NOTE,
}
