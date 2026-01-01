# Defense Share Mod - Together in Spire Addon

Un mod para Slay the Spire que permite compartir cartas de defensa con tus aliados en partidas cooperativas de Together in Spire.

## Descripcion

Este mod extiende la funcionalidad de Together in Spire permitiendo que las cartas que otorgan Block (defensa) puedan ser lanzadas sobre tus companeros de equipo en lugar de solo sobre ti mismo.

## Caracteristicas

- **Targeting de aliados**: Cuando juegas una carta de defensa, puedes hacer click en un aliado para enviarle el bloqueo
- **Deteccion automatica**: El mod detecta automaticamente las cartas que otorgan defensa
- **Compatible con mods**: Funciona con cartas de otros mods que otorguen Block
- **Indicador visual**: Muestra [ALLY] en las cartas que pueden compartirse cuando hay aliados disponibles

## Cartas compatibles

El mod funciona con cualquier carta que otorgue Block, incluyendo:

### Ironclad
- Defend, Shrug It Off, Iron Wave, Impervious, Flame Barrier, etc.

### Silent
- Defend, Dodge and Roll, Blur, Backflip, Cloak and Dagger, etc.

### Defect
- Defend, Glacier, Leap, Chill, Coolheaded, Auto-Shields, etc.

### Watcher
- Defend, Protect, Third Eye, Empty Body, Halt, Spirit Shield, etc.

## Requisitos

- Slay the Spire
- ModTheSpire (3.23.0+)
- BaseMod (5.50.0+)
- Together in Spire

## Instalacion (usuarios)

1. Suscribete al mod en Steam Workshop
2. Activa el mod en ModTheSpire junto con Together in Spire
3. Inicia una partida cooperativa

## Uso en partida

1. Inicia una partida cooperativa con Together in Spire
2. Durante el combate, cuando juegues una carta de defensa:
   - Veras el indicador [ALLY] en cartas que pueden compartirse
   - Haz click en un aliado para enviarle el bloqueo
   - Haz click derecho o ESC para cancelar y aplicar el bloqueo a ti mismo
3. El bloqueo se aplicara al aliado seleccionado

---

## Desarrollo

### Estructura del proyecto

```
DefenseShareMod/
├── pom.xml                              # Configuracion Maven
├── README.md                            # Este archivo
├── setup-libs.sh                        # Script para copiar dependencias
├── upload-to-workshop.sh                # Script para subir a Steam
├── lib/                                 # Dependencias (JARs del juego)
│   ├── desktop-1.0.jar
│   ├── ModTheSpire.jar
│   └── BaseMod.jar
├── workshop/                            # Archivos para Steam Workshop
│   ├── config.json                      # Configuracion del mod en Workshop
│   ├── preview.png                      # Imagen de preview (512x512 o 1280x720)
│   └── content/
│       └── DefenseShareMod.jar          # JAR compilado
└── src/main/
    ├── resources/
    │   └── ModTheSpire.json             # Metadatos del mod
    └── java/defenseshare/
        ├── DefenseShareMod.java         # Clase principal
        ├── config/
        │   └── ModConfig.java           # Configuracion
        ├── patches/
        │   ├── CardTargetingPatch.java  # Modifica targeting de cartas
        │   ├── GainBlockPatch.java      # Redirige block a aliados
        │   └── RenderPatch.java         # Indicadores visuales
        └── util/
            ├── AllyManager.java         # Manejo de aliados de TiS
            └── DefenseCardDetector.java # Detecta cartas de defensa
```

### Requisitos de desarrollo

- Java JDK 8 (requerido por ModTheSpire)
- Maven 3.6+

### Configuracion inicial (una sola vez)

#### 1. Instalar Maven (si no lo tienes)

**Windows:**
Descarga de https://maven.apache.org/download.cgi y agrega al PATH

**Mac:**
```bash
# Con Homebrew (recomendado)
brew install maven

# O descarga manual
cd ~
curl -sL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz -o maven.tar.gz
tar -xzf maven.tar.gz && rm maven.tar.gz
echo 'export PATH="$HOME/apache-maven-3.9.6/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

**Linux/WSL:**
```bash
# Descargar Maven localmente
cd ~
curl -sL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz -o maven.tar.gz
tar -xzf maven.tar.gz && rm maven.tar.gz

# Agregar al PATH (añadir a .bashrc para que sea permanente)
echo 'export PATH="$HOME/apache-maven-3.9.6/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

#### 2. Copiar dependencias a lib/

Los mods de Slay the Spire necesitan los JARs del juego para compilar.

**Ubicaciones segun sistema operativo:**

| Archivo | Windows | Mac | Linux |
|---------|---------|-----|-------|
| desktop-1.0.jar | `C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire\` | `~/Library/Application Support/Steam/steamapps/common/SlayTheSpire/SlayTheSpire.app/Contents/Resources/` | `~/.steam/steam/steamapps/common/SlayTheSpire/` |
| ModTheSpire.jar | `...\workshop\content\646570\1605060445\` | `...\workshop\content\646570\1605060445\` | `...\workshop\content\646570\1605060445\` |
| BaseMod.jar | `...\workshop\content\646570\1605833019\` | `...\workshop\content\646570\1605833019\` | `...\workshop\content\646570\1605833019\` |

**Copiar los JARs en Windows/WSL:**
```bash
STEAM="/mnt/c/Program Files (x86)/Steam/steamapps"

cp "$STEAM/common/SlayTheSpire/desktop-1.0.jar" lib/
cp "$STEAM/workshop/content/646570/1605060445/ModTheSpire.jar" lib/
cp "$STEAM/workshop/content/646570/1605833019/BaseMod.jar" lib/
```

**Copiar los JARs en Mac:**
```bash
STEAM="$HOME/Library/Application Support/Steam/steamapps"

cp "$STEAM/common/SlayTheSpire/SlayTheSpire.app/Contents/Resources/desktop-1.0.jar" lib/
cp "$STEAM/workshop/content/646570/1605060445/ModTheSpire.jar" lib/
cp "$STEAM/workshop/content/646570/1605833019/BaseMod.jar" lib/
```

**Copiar los JARs en Linux:**
```bash
STEAM="$HOME/.steam/steam/steamapps"

cp "$STEAM/common/SlayTheSpire/desktop-1.0.jar" lib/
cp "$STEAM/workshop/content/646570/1605060445/ModTheSpire.jar" lib/
cp "$STEAM/workshop/content/646570/1605833019/BaseMod.jar" lib/
```

### Compilacion

```bash
cd /home/os_uis/projects/DefenseShareMod

# Compilar
mvn clean package

# El JAR se genera en: target/DefenseShareMod.jar
```

### Testing local

Copia el JAR a la carpeta de mods de Slay the Spire:

**Windows/WSL:**
```bash
cp target/DefenseShareMod.jar "/mnt/c/Program Files (x86)/Steam/steamapps/common/SlayTheSpire/mods/"
```

**Mac:**
```bash
cp target/DefenseShareMod.jar "$HOME/Library/Application Support/Steam/steamapps/common/SlayTheSpire/SlayTheSpire.app/Contents/Resources/mods/"
```

**Linux:**
```bash
cp target/DefenseShareMod.jar "$HOME/.steam/steam/steamapps/common/SlayTheSpire/mods/"
```

Luego ejecuta el juego con ModTheSpire y activa el mod.

---

## Subir a Steam Workshop

### Preparacion

1. **Compila el mod** (si no lo has hecho):
   ```bash
   mvn clean package
   ```

2. **Copia el JAR a workshop/content/**:
   ```bash
   cp target/DefenseShareMod.jar workshop/content/
   ```

3. **Asegurate de tener preview.png** en workshop/ (512x512 o 1280x720 px)

4. **Revisa workshop/config.json** con la descripcion del mod

### Subir desde tu sistema operativo

El mod-uploader.jar necesita conectarse a Steam, por lo que debe ejecutarse nativamente (no desde WSL).

#### Windows

```cmd
cd "C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire"
.\jre\bin\java.exe -jar mod-uploader.jar upload -w "C:\Users\TU_USUARIO\Desktop\DefenseShareMod_Workshop"
```

Si usas WSL, primero copia la carpeta workshop a Windows:
```bash
cp -r /home/os_uis/projects/DefenseShareMod/workshop "/mnt/c/Users/José Luis/Desktop/DefenseShareMod_Workshop"
```

#### Mac

```bash
cd "$HOME/Library/Application Support/Steam/steamapps/common/SlayTheSpire/SlayTheSpire.app/Contents/Resources"
./jre/bin/java -jar mod-uploader.jar upload -w ~/Desktop/DefenseShareMod_Workshop
```

Primero copia la carpeta workshop al escritorio:
```bash
cp -r /ruta/a/DefenseShareMod/workshop ~/Desktop/DefenseShareMod_Workshop
```

#### Linux

```bash
cd ~/.steam/steam/steamapps/common/SlayTheSpire
./jre/bin/java -jar mod-uploader.jar upload -w ~/Desktop/DefenseShareMod_Workshop
```

Primero copia la carpeta workshop al escritorio:
```bash
cp -r /ruta/a/DefenseShareMod/workshop ~/Desktop/DefenseShareMod_Workshop
```

### Requisitos para subir

- **Steam debe estar abierto** con tu cuenta logueada
- **Aceptar el acuerdo de Steam Workshop** (primera vez): https://steamcommunity.com/workshop/workshoplegalagreement

### Actualizar el mod

Para actualizar una version existente, simplemente ejecuta el mismo comando de upload. El uploader detectara que ya existe y lo actualizara.

---

## Notas tecnicas

### Como funciona el mod

1. **DefenseShareMod.java**: Clase principal que registra el mod con BaseMod y maneja el flujo general

2. **DefenseCardDetector.java**: Detecta si una carta otorga Block mediante:
   - Lista de cartas conocidas (Defend, Shrug It Off, etc.)
   - Deteccion dinamica (baseBlock > 0)
   - Analisis de descripcion de la carta

3. **AllyManager.java**: Maneja la interaccion con Together in Spire:
   - Detecta TiS mediante reflection (sin dependencia directa)
   - Obtiene lista de aliados disponibles
   - Gestiona la seleccion de aliado con mouse

4. **Patches**: Modifican el comportamiento del juego:
   - `CardTargetingPatch`: Permite que cartas de defensa apunten a aliados
   - `GainBlockPatch`: Redirige GainBlockAction al aliado seleccionado
   - `RenderPatch`: Muestra indicadores visuales [ALLY]

### Compatibilidad

- El mod usa reflection para detectar Together in Spire sin requerir su codigo fuente
- Funciona como add-on separado, no modifica TiS directamente
- Si TiS actualiza su API interna, puede requerir actualizacion

---

## Compartir el mod con amigos

Si el mod está en análisis de Steam o quieres compartirlo antes de publicarlo:

### Opcion 1: Compartir el JAR directamente

Envia el archivo `DefenseShareMod.jar` a tu amigo.

**Ubicacion del JAR compilado:**
- Proyecto: `target/DefenseShareMod.jar`
- Workshop: `workshop/content/DefenseShareMod.jar`

**Tu amigo debe copiar el JAR a su carpeta de mods:**

| Sistema | Carpeta de mods |
|---------|-----------------|
| Windows | `C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire\mods\` |
| Mac | `~/Library/Application Support/Steam/steamapps/common/SlayTheSpire/SlayTheSpire.app/Contents/Resources/mods/` |
| Linux | `~/.steam/steam/steamapps/common/SlayTheSpire/mods/` |

Luego ejecutar el juego con ModTheSpire y activar el mod.

### Opcion 2: Enlace directo de Steam Workshop

Aunque el mod esté oculto/en análisis, puedes compartir el enlace directo:

1. Ve a Steam → Tu perfil → Workshop Items → Tus archivos
2. Busca "Defense Share" y copia el enlace
3. Envia el enlace a tu amigo (puede suscribirse aunque esté oculto)

### Opcion 3: Cambiar visibilidad a "Solo amigos"

1. Ve a Steam → Tu perfil → Workshop Items
2. Encuentra tu mod y click en "Editar"
3. Cambia visibilidad de "Oculto" a "Solo amigos"
4. Tu amigo podra ver y suscribirse al mod

---

## Licencia

Este proyecto es de codigo abierto. Sientete libre de modificarlo y distribuirlo.

## Creditos

- Slay the Spire por Mega Crit Games
- Together in Spire por Draco9990
- ModTheSpire y BaseMod por la comunidad de modding de StS
