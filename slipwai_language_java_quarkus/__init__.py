"""The `java-quarkus` language package: the Java backend whose startup Quarkus owns.

`LANGUAGE` is the object core's loader reads: one backend of the `java` family, which it requires (`language.json`'s
`requires`) and inherits every answer from that it does not give itself.
"""
from __future__ import annotations

from .java_quarkus import LANGUAGE

__all__ = ["LANGUAGE"]
