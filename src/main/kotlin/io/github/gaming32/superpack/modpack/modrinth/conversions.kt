package io.github.gaming32.superpack.modpack.modrinth

import io.github.gaming32.mrpacklib.Mrpack
import io.github.gaming32.superpack.modpack.Compatibility
import io.github.gaming32.superpack.modpack.Side

val Side.mrpack get() = Mrpack.EnvSide.values()[ordinal]

val Mrpack.EnvSide.superpack get() = Side.values()[ordinal]

val Compatibility.mrpack get() = Mrpack.EnvCompatibility.values()[ordinal]

val Mrpack.EnvCompatibility.superpack get() = Compatibility.values()[ordinal]
