#!/usr/bin/env bash
# bench.sh — mide el launcher en la tele y deja los números en DEVICE_DIR.
#
# Uso:
#   scripts/bench.sh [-l etiqueta] [-p paquete] [-a activity] [-n corridas] [--no-compile] [--quick]
#   --quick: 5 arranques, 30 s de reposo, gate al 20% (~1,5 min). Para pasos que no tocan arranque ni reposo.
#   scripts/bench.sh -l "stock" -p com.google.android.tvlauncher -a .MainActivity --no-compile
#
# Escribe una fila en <DEVICE_DIR>/bench.tsv y una sección en <DEVICE_DIR>/metricas.md.
# Sale con código 2 si un tiempo, la CPU o la fluidez empeoran más de 10% contra la corrida anterior del mismo
# paquete, o si la memoria privada o el APK pasan su presupuesto. La memoria y el APK crecen a propósito con
# cada función: su delta se muestra, pero el gate es el presupuesto absoluto.
#
# Variables: SERIAL (adb), DEVICE_DIR (carpeta de resultados, por defecto ./bench-results), NAV (keycodes de la navegación).
set -euo pipefail

SERIAL="${SERIAL:?definir SERIAL=<ip-de-la-tele>:5555}"
DEVICE_DIR="${DEVICE_DIR:-./bench-results}"
mkdir -p "$DEVICE_DIR"
PKG="pakvothe.franlauncher.tv"
ACT=".app.HomeActivity"
LABEL=""
RUNS=10
COMPILE=1
QUICK=0
IDLE_SAMPLES=12
GATE=1.1
# ~30 s de navegación: derecha, izquierda, abajo y arriba (22 21 20 19).
NAV="${NAV:-$(for i in 1 2 3 4 5; do printf '22 22 22 22 22 21 21 21 21 21 20 19 '; done)}"
# En el launcher propio, además: abrir Todas, recorrer 30 ítems, el índice y volver con Atrás.
NAV_LIST="${NAV_LIST:-$(for i in $(seq 30); do printf '20 '; done; for i in $(seq 10); do printf '19 '; done; printf '22 20 20 20 19 21 4')}"

while [ $# -gt 0 ]; do
  case "$1" in
    -l) LABEL="$2"; shift 2 ;;
    -p) PKG="$2"; shift 2 ;;
    -a) ACT="$2"; shift 2 ;;
    -n) RUNS="$2"; shift 2 ;;
    --no-compile) COMPILE=0; shift ;;
    --quick) QUICK=1; RUNS=5; IDLE_SAMPLES=6; GATE=1.2; shift ;;
    *) echo "argumento desconocido: $1" >&2; exit 1 ;;
  esac
done

sh() { adb -s "$SERIAL" shell "$@" | tr -d '\r'; }
log() { printf '· %s\n' "$*" >&2; }

median() { { grep -E '^[0-9.]+$' || true; } | sort -n | awk '{a[NR]=$1} END {if (NR==0) {print "-"; exit} if (NR%2) print a[(NR+1)/2]; else print (a[NR/2]+a[NR/2+1])/2}'; }
# TotalTime de am start; si no lo da (HOME ya vivo, o relanzado por el sistema tras -S), cae a WaitTime.
# Imprime "<ms> <fuente>". WaitTime incluye ~10-30 ms de overhead de am: no es comparable 1 a 1 con TotalTime.
am_time() {
  awk -F': ' '/TotalTime/ {t=$2} /WaitTime/ {w=$2}
    END { if (t != "" && t != 0) print t, "TotalTime"; else if (w != "") print w, "WaitTime" }'
}

mb() { awk -v k="$1" 'BEGIN{ if (k=="" || k=="-") print "-"; else printf "%.1f", k/1024 }'; }

# --- Preflight -------------------------------------------------------------
adb -s "$SERIAL" get-state >/dev/null 2>&1 || adb connect "$SERIAL" >/dev/null
APK_PATH="$(sh pm path "$PKG" | head -1 | sed 's/^package://')"
[ -n "$APK_PATH" ] || { echo "$PKG no está instalado" >&2; exit 1; }
APK_KB="$(sh stat -c %s "$APK_PATH" | awk '{printf "%.1f", $1/1024}')"
HOME_PKG="$(sh cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | cut -d/ -f1)"
NCPU="$(sh cat /sys/devices/system/cpu/present | awk -F- '{print $2+1}')"
FINGERPRINT="$(sh getprop ro.build.fingerprint)"
COMMIT="$(git -C "$(dirname "$0")" rev-parse --short HEAD 2>/dev/null || echo '-')"
log "$PKG ($APK_KB KB) · HOME=$HOME_PKG · $NCPU CPUs · commit $COMMIT"

if [ "$COMPILE" = 1 ]; then
  log "compilando AOT (speed)"
  sh cmd package compile -m speed -f "$PKG" >/dev/null
fi

# --- 1. Arranque en frío ---------------------------------------------------
log "arranque en frío x$RUNS"
COLD=""; FF=""; PROC=""; COLD_SRC=""; DESC=0
for i in $(seq "$RUNS"); do
  # force-stop + pausa, no "am start -S": con -S el arranque pisa la limpieza del proceso viejo y suma ~300 ms.
  sh am force-stop "$PKG"; sleep 1.5
  sh logcat -c
  t=""; read -r t COLD_SRC <<<"$(sh am start -W -n "$PKG/$ACT" | am_time)" || true
  COLD_SRC="${COLD_SRC}+fs"  # método force-stop: no comparable con las filas viejas medidas con -S
  # WaitTime < 50 ms = el intent cayó en una instancia viva (un HOME que el sistema relanzó solo): no es un arranque.
  if [ -n "${t:-}" ] && { [ "$COLD_SRC" = TotalTime ] || [ "$t" -ge 50 ]; }; then COLD="$COLD $t"; else DESC=$((DESC+1)); fi
  sleep 1.5
  # Línea que emite el launcher propio al dibujar el primer frame (no existe en el stock).
  f="$(sh logcat -d -s FranLauncher:I | sed -n 's/.*firstframe=\([0-9]*\).*/\1/p' | tail -1 || true)"
  if [ -n "$f" ]; then FF="$FF $f"; fi
  pr="$(sh logcat -d -s FranLauncher:I | sed -n 's/.*proc=\([0-9]*\).*/\1/p' | tail -1 || true)"
  if [ -n "$pr" ]; then PROC="$PROC $pr"; fi
done
COLD_MED="$(echo $COLD | tr ' ' '\n' | median)"
FF_MED="$(echo $FF | tr ' ' '\n' | median)"
PROC_MED="$(echo $PROC | tr ' ' '\n' | median)"
log "frío: mediana $COLD_MED ms [$COLD_SRC] ($COLD; $DESC descartadas) · primer frame: $FF_MED · proceso→frame: $PROC_MED"

# --- 2. Volver a Home desde otra app ---------------------------------------
HOME_MED="-"; HOME_SRC=""
if [ "$HOME_PKG" = "$PKG" ]; then
  log "volver a Home x$RUNS"
  HT=""
  for i in $(seq "$RUNS"); do
    sh am start -W -a android.settings.SETTINGS >/dev/null; sleep 2
    sh logcat -c
    t=""; read -r t HOME_SRC <<<"$(sh am start -W -a android.intent.action.MAIN -c android.intent.category.HOME | am_time)" || true
    if [ "$HOME_SRC" = WaitTime ]; then
      # Mejor que WaitTime: el "Displayed" de ActivityTaskManager (formato +1s234ms o +234ms), si lo hay.
      d="$(sh logcat -d -s ActivityTaskManager:I | grep "Displayed $PKG" | tail -1 \
        | sed -n 's/.*+\([0-9]*s\)\{0,1\}\([0-9]*\)ms.*/\1 \2/p' | awk '{if (NF==1) print $1; else {sub("s","",$1); print $1*1000+$2}}' || true)"
      if [ -n "$d" ]; then t="$d"; HOME_SRC=Displayed; fi
    fi
    # WaitTime no espera el dibujo de un HOME que vuelve al frente: no sirve como medida.
    if [ -n "$t" ] && [ "$HOME_SRC" != WaitTime ]; then HT="$HT $t"; fi
    sleep 1
  done
  HOME_MED="$(echo $HT | tr ' ' '\n' | median)"
  if [ "$HOME_MED" = - ]; then HOME_SRC="sin método: am no lo reporta"; fi
  log "home: mediana $HOME_MED ms [$HOME_SRC] ($HT)"
else
  log "volver a Home: salteado ($PKG no es el HOME actual)"
fi

# --- 3. Fluidez ------------------------------------------------------------
sh am start -W -n "$PKG/$ACT" >/dev/null; sleep 5
# Calentamiento: el primer uso tras arrancar paga glifos, ICU y capas una sola vez (el launcher vive días).
# Se mide aparte como "primer uso" y la fluidez de régimen sale de la pasada siguiente.
sh dumpsys gfxinfo "$PKG" reset >/dev/null
sh "for k in 22 22 22 21 21 21; do input keyevent \$k; done"
if [ "$PKG" = pakvothe.franlauncher.tv ]; then
  sh am start -n "$PKG/$ACT" --es fl.open all >/dev/null; sleep 1
  sh "for k in 20 20 20 19 22 20 21 4; do input keyevent \$k; done"; sleep 1
fi
WARM_JANK="$(sh dumpsys gfxinfo "$PKG" | sed -n 's/.*Janky frames: \([0-9]*\) (\([0-9.]*\)%).*/\1 de las primeras, \2%/p' | head -1)"
log "primer uso: $WARM_JANK"
sh dumpsys gfxinfo "$PKG" reset >/dev/null
log "navegando ($(echo $NAV | wc -w | tr -d ' ') teclas)"
sh "for k in $NAV; do input keyevent \$k; done"
if [ "$PKG" = pakvothe.franlauncher.tv ]; then
  sh am start -n "$PKG/$ACT" --es fl.open all >/dev/null; sleep 1
  sh "for k in $NAV_LIST; do input keyevent \$k; done"
fi
sleep 1
GFX="$(sh dumpsys gfxinfo "$PKG")"
FRAMES="$(echo "$GFX" | awk -F': ' '/Total frames rendered/ {print $2; exit}')"
JANK="$(echo "$GFX" | sed -n 's/.*Janky frames: [0-9]* (\([0-9.]*\)%).*/\1/p' | head -1)"
P50="$(echo "$GFX" | awk -F': ' '/^50th percentile/ {sub("ms","",$2); print $2; exit}')"
P90="$(echo "$GFX" | awk -F': ' '/^90th percentile/ {sub("ms","",$2); print $2; exit}')"
P99="$(echo "$GFX" | awk -F': ' '/^99th percentile/ {sub("ms","",$2); print $2; exit}')"
# Sin frames, gfxinfo imprime percentiles basura.
if [ "${FRAMES:-0}" = 0 ]; then JANK=""; P50=""; P90=""; P99=""; fi
log "fluidez: $FRAMES frames, $JANK% lentos, p50/p90/p99 $P50/$P90/$P99 ms"

# --- 4. Reposo: memoria, CPU y allocations (misma ventana de 60 s) ----------
meminfo() {
  sh dumpsys meminfo "$PKG" | awk '
    /App Summary/ {s=1}
    s && /Java Heap:/   {j=$3}
    s && /Native Heap:/ {n=$3}
    s && /^ *Code:/     {c=$2}
    s && /Graphics:/    {g=$2}
    s && /TOTAL:/       {t=$2; print j, n, c, g, t; exit}'
}
# El runtime crece ~1,2 MB una sola vez en los primeros ~30 s tras abrir: la ventana arranca después.
# Y el primer dumpsys en el proceso también asigna: una lectura descartable antes.
meminfo >/dev/null
sleep 30
read -r J0 N0 C0 G0 T0 <<<"$(meminfo)"
PID="$(sh pidof "$PKG")"
log "reposo $((IDLE_SAMPLES * 5)) s (pid $PID)"
CPU="$(sh top -b -q -d 5 -n "$IDLE_SAMPLES" -p "$PID" -o %CPU | awk 'NF {s+=$1; n++} END {if (n) printf "%.2f", s/n; else print "-"}')"
read -r J1 N1 C1 G1 T1 <<<"$(meminfo)"
PRIV=$((J1 + N1 + C1))
HEAP_DELTA=$((J1 - J0))
log "memoria: privado $(mb $PRIV) MB (J $(mb $J1) · N $(mb $N1) · C $(mb $C1)) · Graphics $(mb $G1) · TOTAL $(mb $T1) MB · heap Δ ${HEAP_DELTA} KB · CPU $CPU% (sobre $((NCPU*100))%)"

# --- 5. Guardar y comparar -------------------------------------------------
TSV="$DEVICE_DIR/bench.tsv"
COLS="fecha\tfuente_tiempos\tetiqueta\tpaquete\tcommit\tapk_kb\tfrio_ms\tprimer_frame_ms\thome_ms\tprivado_kb\tjava_kb\tnative_kb\tcode_kb\tgraphics_kb\ttotal_kb\tcpu_pct\tjank_pct\tp50\tp90\tp99\tframes\theap_delta_kb\tproc_frame_ms"
[ -f "$TSV" ] || printf "$COLS\n" > "$TSV"
NOW="$(date '+%Y-%m-%d %H:%M')"
ROW="$NOW\t${COLD_SRC:--}/${HOME_SRC:--}\t${LABEL:--}\t$PKG\t$COMMIT\t$APK_KB\t$COLD_MED\t$FF_MED\t$HOME_MED\t$PRIV\t$J1\t$N1\t$C1\t$G1\t$T1\t$CPU\t${JANK:--}\t${P50:--}\t${P90:--}\t${P99:--}\t${FRAMES:--}\t$HEAP_DELTA\t$PROC_MED"
PREV="$(awk -F'\t' -v p="$PKG" -v src="${COLD_SRC:--}/${HOME_SRC:--}" 'NR>1 && $4==p && $2==src' "$TSV" | tail -1)"
printf "$ROW\n" >> "$TSV"

# Métricas donde menos es mejor: columna del TSV → nombre.
REGRESION=""
if [ -n "$PREV" ]; then
  NEW="$(printf "$ROW")"
  REGRESION="$(awk -F'\t' -v prev="$PREV" -v new="$NEW" -v gate="$GATE" 'BEGIN {
    split(prev, a, "\t"); split(new, b, "\t")
    # El TotalTime del frío no frena: incluye pausar la app anterior (el launcher de stock varía ±350 ms).
    n = split("22:proceso→frame 8:primer_frame 9:home", m, " ")
    for (k = 1; k <= n; k++) { split(m[k], kv, ":"); c=kv[1]
      if (a[c] ~ /^[0-9.]+$/ && b[c] ~ /^[0-9.]+$/ && a[c] > 0 && b[c] > a[c]*gate)
        printf "%s %s → %s (+%.0f%%); ", substr(m[k], length(c)+2), a[c], b[c], (b[c]/a[c]-1)*100 }
  }')"
fi

{
  printf '\n## Bench %s%s (%s, commit %s)\n' "$NOW" "${LABEL:+ · $LABEL}" "$PKG" "$COMMIT"
  printf '| Métrica | Valor | Objetivo |\n|---|---|---|\n'
  printf '| APK | %s KB | < 250 KB |\n' "$APK_KB"
  printf '| Arranque en frío (mediana de %s, %s) | %s ms | < 300 ms |\n' "$RUNS" "${COLD_SRC:--}" "$COLD_MED"
  printf '| onCreate → primer frame | %s ms | ≤ 100 ms |\n' "$FF_MED"
  printf '| Proceso creado → primer frame | %s ms | (gate del frío) |\n' "$PROC_MED"
  printf '| Volver a Home (%s) | %s ms | < 150 ms |\n' "${HOME_SRC:--}" "$HOME_MED"
  printf '| Privado (Java + Native + Code) | %s MB (%s + %s + %s) | < 15 MB |\n' "$(mb $PRIV)" "$(mb $J1)" "$(mb $N1)" "$(mb $C1)"
  printf '| Graphics / TOTAL PSS | %s / %s MB | ≤ 1/5 del stock |\n' "$(mb $G1)" "$(mb $T1)"
  printf '| CPU en reposo (%s s) | %s%% (escala %s%%) | < 2%% |\n' "$((IDLE_SAMPLES * 5))" "$CPU" "$((NCPU*100))"
  printf '| Fluidez | %s%% lentos de %s frames; p50 %s, p90 %s, p99 %s ms | < 1%%, p99 < 16 ms |\n' "${JANK:--}" "${FRAMES:--}" "${P50:--}" "${P90:--}" "${P99:--}"
  printf '| Heap Java en reposo | Δ %s KB | 0 |\n' "$HEAP_DELTA"
  printf '| Frames lentos en el primer uso (calentamiento) | %s | (informativo) |\n' "${WARM_JANK:--}"
} >> "$DEVICE_DIR/metricas.md"

# Fluidez: solo frena si sale de objetivo (con pocos frames, el p99 salta de 5 a 8 ms sin significar nada).
awk -v j="${JANK:-0}" -v p="${P99:-0}" 'BEGIN{exit !(j >= 1 || p >= 16)}' && REGRESION="${REGRESION}fluidez ${JANK}% lentos, p99 ${P99} ms; "
DELTAS=""
if [ -n "$PREV" ]; then
  DELTAS="$(awk -F'\t' -v prev="$PREV" -v new="$NEW" 'BEGIN { split(prev, a, "\t"); split(new, b, "\t")
    n = split("6:APK_KB 10:privado_KB 14:graphics_KB 15:TOTAL_PSS_KB", m, " ")
    for (k = 1; k <= n; k++) { split(m[k], kv, ":"); c = kv[1]
      if (a[c] ~ /^[0-9.]+$/ && b[c] ~ /^[0-9.]+$/ && a[c] > 0) printf "%s %+.0f%%; ", kv[2], (b[c]/a[c]-1)*100 } }')"
fi
# CPU: gate absoluto (objetivo < 2%); con ~4 frames/s una ventana corta varía ±0,4 puntos.
awk -v c="$CPU" 'BEGIN{exit !(c >= 2)}' && REGRESION="${REGRESION}CPU $CPU% ≥ 2%; "
awk -v p="$PRIV" 'BEGIN{exit !(p >= 15*1024)}' && REGRESION="${REGRESION}privado $(mb $PRIV) MB ≥ 15 MB; "
awk -v k="$APK_KB" 'BEGIN{exit !(k >= 250)}' && REGRESION="${REGRESION}APK $APK_KB KB ≥ 250 KB; "
if [ -n "$DELTAS" ]; then log "deltas de tamaño: $DELTAS"; printf '\nDeltas de tamaño contra la corrida anterior: %s\n' "$DELTAS" >> "$DEVICE_DIR/metricas.md"; fi
if [ -n "$REGRESION" ]; then printf '\n⚠️ **Fuera de objetivo:** %s\n' "$REGRESION" >> "$DEVICE_DIR/metricas.md"; fi

# Si cambió el build de la tele (OTA), avisar: hay que revisar aplicado.sh.
grep -qF "$FINGERPRINT" "$DEVICE_DIR/changelog.md" 2>/dev/null || log "⚠️ fingerprint nuevo ($FINGERPRINT): posible OTA, revisar pm list packages -e"

log "escrito en metricas.md y bench.tsv"
if [ -n "$REGRESION" ]; then echo "REGRESIÓN: $REGRESION" >&2; exit 2; fi
