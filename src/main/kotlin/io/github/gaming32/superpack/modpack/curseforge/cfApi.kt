package io.github.gaming32.superpack.modpack.curseforge

import io.github.matyrobbrt.curseforgeapi.CurseForgeAPI
import java.util.concurrent.Semaphore

val CF_SEMAPHORE = Semaphore(128)

val CF_API: CurseForgeAPI = CurseForgeAPI.builder()
    .apiKey("\$2a\$10\$jx5IHWHDU.tTYMPu02/Z9efs6wZVU3hNpby3IWyJRQoo8dgB0Ae2.")
    .build()
