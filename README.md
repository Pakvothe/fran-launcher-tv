# Fran Launcher

Launcher ultraliviano para Android TV: un reloj sobre un cielo que cambia con la hora, tus apps y nada más. Sin datos, sin anuncios, sin cuentas y sin permiso de internet.

Hecho por [Pakvothe](https://github.com/Pakvothe).

## Por qué

El launcher de stock de Android TV ocupa más de 100 MB, carga recomendaciones y banners remotos y tironea al navegar. Fran Launcher hace menos a propósito: dibuja todo localmente, desde el disco, en una sola View nativa.

Medido en una Admiral 4K (Android TV 10, UI a 1080p) contra el launcher de stock, con todas las funciones activas:

| | Fran Launcher | Stock |
|---|---|---|
| Tamaño del APK | **141 KB** | 12,7 MB |
| RAM privada tras 10 min de uso | **11 MB** | 54-62 MB |
| Frames lentos al navegar | **0%** (p99 9 ms) | 5,9% (p99 30 ms) |
| Arranque en frío | **285 ms** | 625-712 ms |
| Volver a Home (costo propio) | **3-8 ms** | sin dato |
| CPU en reposo (modo ambiente) | **0,03%** sin segundero · 0,56% con segundero | ~0,01% |

Con el launcher de stock deshabilitado, la tele pasó de 804 MB a 938 MB de RAM disponible a los 2 minutos de un arranque limpio.

## Qué tiene

- **Cielo de la hora**: degradado y dos orbes de luz difusos que siguen la hora local (noche, amanecer, día, atardecer), con crossfade al cambiar de fase.
- **Reloj** grande con cifras tabulares, segundero (número, línea o apagado), fecha y formato 12/24 h.
- **Fila de favoritas** con banners, foco animado, nombre bajo el foco y el foco en la última app usada.
- **Todas las apps** en un panel lateral: lista estilo rueda selectora, índice de letras y menú de pulsación larga (mover, quitar o agregar al inicio, info, desinstalar).
- **Entradas** HDMI y AV, con estado de señal (solo si la tele tiene entradas).
- **Reproduciendo**: qué suena, con carátula y play/pausa. Opcional; necesita acceso a notificaciones, que solo se usa para leer la sesión de medios.
- **Modo ambiente** tras unos segundos sin tocar nada, con protección OLED (el reloj se desplaza cada minuto) y atenuación nocturna.
- **Tema** claro, oscuro o automático según la hora.
- **Idiomas**: español, inglés y portugués, con selector propio en Ajustes.
- **Accesibilidad**: nodos virtuales para TalkBack.

## Cómo está hecho

- **Java puro**, `minSdk 29`, sin Kotlin, sin AndroidX y sin dependencias de terceros.
- **Una Activity, una View y un árbol de `RenderNode`**: nada de layouts XML, `RecyclerView` ni `Fragment`. El foco y las animaciones son solo propiedades de nodos ya grabados.
- **Motor de animación propio** sobre `Choreographer`, con re-targeting y sin allocations por frame.
- **Cero trabajo en reposo**: sin polling; los orbes y el reloj solo piden frames cuando algo cambió de verdad.
- **Arranque desde un snapshot binario**: la lista de apps, la configuración y los atlas de banners e íconos se leen en crudo (`FileChannel.map`), sin decodificar imágenes. La consulta a `PackageManager` corre después del primer frame, en un único hilo de I/O.
- **Lógica testeable en la JVM**: la máquina de foco, el formato del snapshot, las fases del cielo y la secuencia OLED viven en el módulo `core`, sin APIs de Android (41 tests).

```
launcher/
  app/       La app Android (HomeActivity, Stage, capas de la escena, datos, medios, TV)
  core/      Lógica en Java plano + tests JUnit
  scripts/   bench.sh: medición reproducible en el dispositivo
```

## Compilar

Requiere JDK 17 y el Android SDK (platform 36).

```sh
cd launcher
./gradlew :core:test                                  # tests en la JVM
./gradlew :app:assembleRelease                        # APK sin el filtro HOME
./gradlew :app:assembleRelease -PhomeEnabled=true     # APK que puede ser el inicio
```

El filtro HOME vive en un `activity-alias` apagado por defecto, para poder probar la app al lado del launcher de stock sin que la tele muestre un selector al apretar Home.

## Instalar y dejarlo como inicio

```sh
adb connect <IP de la tele>:5555
adb install -r launcher/app/build/outputs/apk/release/app-release.apk
adb shell cmd package compile -m speed -f pakvothe.franlauncher.tv
adb shell cmd package set-home-activity pakvothe.franlauncher.tv/.Home
```

En Google TV y en varias teles con Android TV, el botón Home vuelve igual al launcher de stock porque su filtro tiene prioridad de sistema. En ese caso hay que deshabilitarlo (reversible):

```sh
adb shell pm disable-user --user 0 com.google.android.tvlauncher   # deshabilitar
adb shell pm enable com.google.android.tvlauncher                  # volver atrás
```

Una actualización del sistema puede volver a habilitar el launcher original.

Para "Mostrar lo que suena" en teles que no tienen la pantalla de acceso a notificaciones:

```sh
adb shell cmd notification allow_listener pakvothe.franlauncher.tv/pakvothe.franlauncher.tv.media.MediaAccessService
```

## Medir

```sh
launcher/scripts/bench.sh -l "etiqueta"            # completo, ~4 min
launcher/scripts/bench.sh --quick -l "etiqueta"    # rápido, ~1:45
launcher/scripts/bench.sh -l stock -p com.google.android.tvlauncher -a .MainActivity --no-compile
```

Mide arranque en frío, onCreate y proceso hasta el primer frame, volver a Home, fluidez navegando la fila y la lista, memoria desglosada, CPU y heap en reposo. Escribe los resultados en una tabla y sale con código 2 si un tiempo empeora más de 10% contra la corrida anterior o si la memoria o el APK pasan su presupuesto. Manda teclas por adb: no usar la tele mientras corre.

## Privacidad

La app no pide internet, ubicación ni almacenamiento, y no usa `QUERY_ALL_PACKAGES`. El único permiso es `REQUEST_DELETE_PACKAGES`, para desinstalar apps desde el menú. La configuración no sale del dispositivo (`allowBackup="false"`). El acceso a notificaciones es opcional y el servicio está vacío: solo habilita ver qué se está reproduciendo.

## Créditos

Fuente [Outfit](https://github.com/Outfitio/Outfit-Fonts), bajo la SIL Open Font License 1.1 (el texto completo está en Ajustes > Licencias).
