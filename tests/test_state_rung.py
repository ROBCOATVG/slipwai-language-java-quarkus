"""Both write-model rungs, generated from this package beside its family: the state rung gets the repository port
and the adapters behind it instead of the event log, and the event rung is left exactly as it was.

Generation only, so it runs in conformance time. The native gates — `make verify` on every store, and
`make migrate test-integration` against a real Postgres — are the matrix's, and the README says how to run them
by hand.
"""
from __future__ import annotations

import tempfile
from pathlib import Path

from support import FactoryTestCase

BACKEND = "java-quarkus"
HTTP = "quarkus-rest"


class StateRungTest(FactoryTestCase):
    def main_package(self, directory: str, rung: str, persistence: str) -> Path:
        """The generated service's main package directory, whatever the project's name made of it."""
        repo = self.generate(directory, f"{rung}-{persistence}", language=BACKEND, write_model=rung,
                             persistence=persistence, http=HTTP, target="none")
        return next((repo / "apps/service/src/main/java/com/example").iterdir())

    def test_state_selects_the_repository_and_no_event_history_for_every_store(self) -> None:
        for persistence in ("memory", "sqlite", "postgres"):
            with self.subTest(persistence=persistence), tempfile.TemporaryDirectory() as directory:
                main = self.main_package(directory, "state", persistence)
                service = main.parents[5]
                self.assertTrue((main / "application/ports/state/Repository.java").is_file())
                self.assertTrue((main / "adapters/driven/repositorymemory/InMemoryRepository.java").is_file())
                self.assertFalse((main / "application/ports/events").exists())
                self.assertFalse((main / "projections").exists())
                self.assertEqual((main / "adapters/driven/repositorysqlite").is_dir(), persistence == "sqlite")
                migrations = service / "src/main/resources/db/migration"
                if persistence == "postgres":
                    self.assertEqual(sorted(path.name for path in migrations.iterdir()), ["V1__states.sql"])
                    self.assertTrue((main / "config/DatabaseUrl.java").is_file())
                    self.assertTrue((main / "migrations/MigrateMain.java").is_file())
                    self.assertFalse((main / "adapters/driven/sql").exists())
                else:
                    self.assertFalse(migrations.exists())

    def test_events_keep_the_log_and_gain_no_repository(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            main = self.main_package(directory, "events", "postgres")
            self.assertTrue((main / "application/ports/events/EventStore.java").is_file())
            self.assertFalse((main / "application/ports/state").exists())

    def test_the_durable_event_store_brings_the_seed_door_and_nothing_else_does(self) -> None:
        """A slice's `try-it.json` may give the events its unbuilt upstream would have written; they go in through
        the event store, by a process with the store's credentials, never a route."""
        with tempfile.TemporaryDirectory() as directory:
            main = self.main_package(directory, "events", "postgres")
            repo = main.parents[7]
            self.assertTrue((main / "seed/SeedMain.java").is_file())
            self.assertIn("exec:java@seed -Dexec.args=$(abspath $(FILE))", (repo / "Makefile").read_text())
            self.assertIn("seed.SeedMain</mainClass>", (repo / "apps/service/pom.xml").read_text())
        for rung, persistence in (("events", "memory"), ("state", "postgres")):
            with self.subTest(rung=rung, persistence=persistence), tempfile.TemporaryDirectory() as directory:
                main = self.main_package(directory, rung, persistence)
                self.assertFalse((main / "seed").exists())
                self.assertNotIn("seed-service:", (main.parents[7] / "Makefile").read_text())
