#!/bin/bash

# Script para copiar las dependencias necesarias a lib/
# Ejecutar UNA VEZ antes de compilar

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LIB_DIR="$SCRIPT_DIR/lib"

echo "=== Defense Share Mod - Setup de dependencias ==="
echo ""

# Detectar sistema operativo y rutas de Steam
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    if grep -q Microsoft /proc/version 2>/dev/null; then
        # WSL - Windows
        STEAM_BASE="/mnt/c/Program Files (x86)/Steam/steamapps"
        echo "Detectado: WSL (Windows)"
    else
        # Linux nativo
        STEAM_BASE="$HOME/.steam/steam/steamapps"
        echo "Detectado: Linux nativo"
    fi
elif [[ "$OSTYPE" == "darwin"* ]]; then
    STEAM_BASE="$HOME/Library/Application Support/Steam/steamapps"
    echo "Detectado: macOS"
else
    echo "Sistema no reconocido. Por favor copia los archivos manualmente."
    exit 1
fi

# Rutas de los archivos
STS_DIR="$STEAM_BASE/common/SlayTheSpire"
WORKSHOP_DIR="$STEAM_BASE/workshop/content/646570"
MTS_DIR="$WORKSHOP_DIR/1605060445"   # ModTheSpire Workshop ID
BASEMOD_DIR="$WORKSHOP_DIR/1605833019"  # BaseMod Workshop ID

echo ""
echo "Buscando archivos en:"
echo "  - Juego: $STS_DIR"
echo "  - Workshop: $WORKSHOP_DIR"
echo ""

# Crear directorio lib si no existe
mkdir -p "$LIB_DIR"

# Función para copiar archivo
copy_file() {
    local src="$1"
    local dest="$2"
    local name="$3"

    if [ -f "$src" ]; then
        cp "$src" "$dest"
        echo "[OK] $name copiado"
        return 0
    else
        echo "[ERROR] No encontrado: $src"
        return 1
    fi
}

ERRORS=0

# 1. desktop-1.0.jar (del juego)
echo "Copiando desktop-1.0.jar..."
copy_file "$STS_DIR/desktop-1.0.jar" "$LIB_DIR/desktop-1.0.jar" "desktop-1.0.jar" || ERRORS=$((ERRORS+1))

# 2. ModTheSpire.jar (del Workshop o del juego)
echo "Copiando ModTheSpire.jar..."
if [ -f "$MTS_DIR/ModTheSpire.jar" ]; then
    copy_file "$MTS_DIR/ModTheSpire.jar" "$LIB_DIR/ModTheSpire.jar" "ModTheSpire.jar"
elif [ -f "$STS_DIR/ModTheSpire.jar" ]; then
    copy_file "$STS_DIR/ModTheSpire.jar" "$LIB_DIR/ModTheSpire.jar" "ModTheSpire.jar"
else
    echo "[ERROR] ModTheSpire.jar no encontrado"
    ERRORS=$((ERRORS+1))
fi

# 3. BaseMod.jar (del Workshop)
echo "Copiando BaseMod.jar..."
if [ -f "$BASEMOD_DIR/BaseMod.jar" ]; then
    copy_file "$BASEMOD_DIR/BaseMod.jar" "$LIB_DIR/BaseMod.jar" "BaseMod.jar"
else
    # Buscar en subcarpetas del workshop
    BASEMOD_JAR=$(find "$WORKSHOP_DIR" -name "BaseMod.jar" 2>/dev/null | head -1)
    if [ -n "$BASEMOD_JAR" ]; then
        copy_file "$BASEMOD_JAR" "$LIB_DIR/BaseMod.jar" "BaseMod.jar"
    else
        echo "[ERROR] BaseMod.jar no encontrado"
        ERRORS=$((ERRORS+1))
    fi
fi

echo ""
echo "=== Resumen ==="
ls -la "$LIB_DIR"

if [ $ERRORS -gt 0 ]; then
    echo ""
    echo "ATENCION: $ERRORS archivo(s) no encontrado(s)"
    echo ""
    echo "Copia manualmente los archivos faltantes a: $LIB_DIR"
    echo ""
    echo "Ubicaciones tipicas:"
    echo "  desktop-1.0.jar: [Steam]/steamapps/common/SlayTheSpire/"
    echo "  ModTheSpire.jar: [Steam]/steamapps/workshop/content/646570/1605060445/"
    echo "  BaseMod.jar:     [Steam]/steamapps/workshop/content/646570/1605833019/"
    exit 1
else
    echo ""
    echo "Todas las dependencias copiadas correctamente!"
    echo "Ahora puedes compilar con: mvn clean package"
fi
