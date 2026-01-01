#!/bin/bash

# Script para subir Defense Share Mod a Steam Workshop
# Uso: ./upload-to-workshop.sh /ruta/a/SlayTheSpire

set -e

# Verificar argumentos
if [ -z "$1" ]; then
    echo "Uso: $0 /ruta/a/SlayTheSpire"
    echo ""
    echo "Ejemplo:"
    echo "  Windows (WSL): $0 '/mnt/c/Program Files (x86)/Steam/steamapps/common/SlayTheSpire'"
    echo "  Linux:         $0 ~/.steam/steam/steamapps/common/SlayTheSpire"
    exit 1
fi

STS_DIR="$1"
MOD_DIR="$(cd "$(dirname "$0")" && pwd)"
UPLOADER="$STS_DIR/mod-uploader.jar"

# Verificar que existe el uploader
if [ ! -f "$UPLOADER" ]; then
    echo "Error: No se encontro mod-uploader.jar en $STS_DIR"
    echo "Asegurate de que la ruta a Slay the Spire es correcta"
    exit 1
fi

echo "=== Defense Share Mod - Workshop Uploader ==="
echo ""

# Paso 1: Compilar el mod
echo "[1/4] Compilando el mod..."
cd "$MOD_DIR"
if command -v mvn &> /dev/null; then
    mvn clean package -q
else
    echo "Error: Maven no esta instalado"
    echo "Instala Maven con: sudo apt install maven"
    exit 1
fi

# Paso 2: Copiar JAR a workshop/content
echo "[2/4] Copiando JAR a workshop/content..."
cp "$MOD_DIR/target/DefenseShareMod.jar" "$MOD_DIR/workshop/content/"

# Paso 3: Verificar preview.png
echo "[3/4] Verificando preview.png..."
if [ ! -f "$MOD_DIR/workshop/preview.png" ]; then
    echo ""
    echo "ATENCION: Falta preview.png en workshop/"
    echo "Steam Workshop requiere una imagen de preview."
    echo "Crea una imagen de 512x512 o 1280x720 pixeles y guardala como:"
    echo "  $MOD_DIR/workshop/preview.png"
    echo ""
    read -p "Presiona Enter cuando hayas agregado la imagen, o Ctrl+C para cancelar..."
fi

# Paso 4: Subir a Workshop
echo "[4/4] Subiendo a Steam Workshop..."
cd "$MOD_DIR/workshop"
java -jar "$UPLOADER" upload -w .

echo ""
echo "=== Subida completada ==="
echo "Tu mod deberia estar disponible en Steam Workshop"
echo "Revisa: https://steamcommunity.com/app/646570/workshop/"
