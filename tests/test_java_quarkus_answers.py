"""What the Quarkus framework answers for itself, in a project generated from this package beside its family.

The family's files arrive too (the Maven wrapper, CRLF intact), read from the `java` package this one requires;
what is Quarkus's own — the Jib image build, the placeholder `make mutation` — is this package's.
"""
from __future__ import annotations

import tempfile

from support import FactoryTestCase


class QuarkusAnswersTest(FactoryTestCase):
    def test_a_quarkus_project_builds_its_image_with_jib_and_explains_its_mutation_target(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            repo = self.generate(directory, "quarkus-own", language="java-quarkus", target="aws")
            self.assertIn("quarkus-container-image-jib", (repo / "apps/service/pom.xml").read_text())
            makefile = (repo / "Makefile").read_text()
            self.assertIn("-Dquarkus.container-image.build=true", makefile)
            self.assertIn("Configure PIT for the domain packages only", makefile)
            self.assertTrue((repo / "apps/service/mvnw.cmd").read_bytes().endswith(b"\r\n"))
