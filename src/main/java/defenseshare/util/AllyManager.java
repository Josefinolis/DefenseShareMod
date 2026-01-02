package defenseshare.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manejador de aliados para Together in Spire
 * Detecta y permite seleccionar aliados en partidas cooperativas
 *
 * OPTIMIZADO: Sin reflection en tiempo de ejecución, solo caching
 */
public class AllyManager {

    private static final Logger logger = LogManager.getLogger(AllyManager.class.getName());

    // Estado de la selección
    private static boolean isSelecting = false;
    private static AbstractPlayer selectedAlly = null;
    private static AbstractPlayer hoveredAlly = null;

    // Cache de aliados - se actualiza solo en eventos específicos
    private static List<AbstractPlayer> cachedAllies = new ArrayList<>();
    private static boolean cacheValid = false;

    // Detección de Together in Spire (se hace UNA vez al inicio)
    private static boolean tisDetected = false;
    private static boolean tisInitialized = false;

    // Accessors pre-compilados para evitar reflection en runtime
    private static Field allCharacterEntitiesField = null;
    private static Method getPlayersMethod = null;
    private static boolean accessorsInitialized = false;

    // Configuración visual
    private static final Color HIGHLIGHT_COLOR = new Color(0.3f, 0.8f, 1.0f, 0.5f);
    private static final Color SELECTED_COLOR = new Color(0.2f, 1.0f, 0.2f, 0.7f);

    public static void initialize() {
        logger.info("Inicializando AllyManager...");
        initializeAccessors();
    }

    /**
     * Inicializa los accessors UNA SOLA VEZ para evitar reflection repetida
     */
    private static void initializeAccessors() {
        if (accessorsInitialized) {
            return;
        }
        accessorsInitialized = true;

        try {
            Class<?> tisClass = Class.forName("spireTogether.SpireTogetherMod");
            tisDetected = true;
            tisInitialized = true;
            logger.info("Together in Spire detectado");

            // Pre-cargar el Field de allCharacterEntities
            try {
                allCharacterEntitiesField = tisClass.getDeclaredField("allCharacterEntities");
                allCharacterEntitiesField.setAccessible(true);
                logger.info("Campo allCharacterEntities encontrado");
            } catch (NoSuchFieldException e) {
                // Intentar con otros nombres de campo conocidos
                String[] fieldNames = {"players", "remotePlayers", "otherPlayers"};
                for (String name : fieldNames) {
                    try {
                        allCharacterEntitiesField = tisClass.getDeclaredField(name);
                        allCharacterEntitiesField.setAccessible(true);
                        logger.info("Campo " + name + " encontrado");
                        break;
                    } catch (NoSuchFieldException ignored) {}
                }
            }

            // Pre-cargar método getter si existe
            try {
                getPlayersMethod = tisClass.getMethod("getPlayers");
                logger.info("Método getPlayers encontrado");
            } catch (NoSuchMethodException ignored) {}

        } catch (ClassNotFoundException e) {
            tisDetected = false;
            tisInitialized = true;
            logger.info("Together in Spire no encontrado");
        }
    }

    /**
     * Verifica si hay aliados disponibles (usa cache)
     */
    public static boolean hasAlliesAvailable() {
        if (!tisDetected) {
            return false;
        }

        // Usar cache si es válido
        if (cacheValid) {
            return !cachedAllies.isEmpty();
        }

        // Actualizar cache solo si es necesario
        refreshAlliesCache();
        return !cachedAllies.isEmpty();
    }

    /**
     * Invalida el cache - llamar cuando cambie el estado del juego
     */
    public static void invalidateCache() {
        cacheValid = false;
    }

    /**
     * Refresca el cache de aliados
     * Solo debe llamarse en eventos específicos, NO cada frame
     */
    public static void refreshAlliesCache() {
        cachedAllies.clear();

        if (!tisDetected || AbstractDungeon.player == null) {
            cacheValid = true;
            return;
        }

        try {
            // Método 1: Usar el campo pre-cargado
            if (allCharacterEntitiesField != null) {
                Object value = allCharacterEntitiesField.get(null);
                extractPlayersFromObject(value);
            }

            // Método 2: Usar el método pre-cargado si no encontramos aliados
            if (cachedAllies.isEmpty() && getPlayersMethod != null) {
                Object result = getPlayersMethod.invoke(null);
                extractPlayersFromObject(result);
            }

        } catch (Exception e) {
            // Silenciar errores en producción
        }

        cacheValid = true;
    }

    /**
     * Extrae jugadores de un objeto (HashMap o List)
     */
    @SuppressWarnings("unchecked")
    private static void extractPlayersFromObject(Object obj) {
        if (obj == null) return;

        try {
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                for (Object value : map.values()) {
                    addPlayerIfValid(value);
                }
            } else if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                for (Object item : list) {
                    addPlayerIfValid(item);
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Añade un jugador a la lista si es válido y no es el jugador actual
     */
    private static void addPlayerIfValid(Object obj) {
        if (obj instanceof AbstractPlayer) {
            AbstractPlayer p = (AbstractPlayer) obj;
            if (p != AbstractDungeon.player && p.currentHealth > 0) {
                cachedAllies.add(p);
            }
        }
    }

    /**
     * Inicia el modo de selección de aliado
     */
    public static void startAllySelection() {
        refreshAlliesCache();
        if (!cachedAllies.isEmpty()) {
            isSelecting = true;
            selectedAlly = null;
            hoveredAlly = null;
        }
    }

    /**
     * Finaliza el modo de selección de aliado
     */
    public static void endAllySelection() {
        isSelecting = false;
        selectedAlly = null;
        hoveredAlly = null;
    }

    /**
     * Actualiza la selección de aliado basada en input del ratón
     */
    public static boolean updateAllySelection() {
        if (!isSelecting || cachedAllies.isEmpty()) {
            return false;
        }

        hoveredAlly = null;
        float mouseX = InputHelper.mX;
        float mouseY = InputHelper.mY;

        for (AbstractPlayer ally : cachedAllies) {
            if (ally.hb != null && ally.hb.hovered) {
                hoveredAlly = ally;
                break;
            }

            // Verificación alternativa
            float hitboxWidth = 150 * Settings.scale;
            float hitboxHeight = 200 * Settings.scale;

            if (mouseX >= ally.drawX - hitboxWidth / 2 && mouseX <= ally.drawX + hitboxWidth / 2 &&
                mouseY >= ally.drawY - hitboxHeight / 2 && mouseY <= ally.drawY + hitboxHeight / 2) {
                hoveredAlly = ally;
                break;
            }
        }

        if (hoveredAlly != null && InputHelper.justClickedLeft) {
            selectedAlly = hoveredAlly;
            InputHelper.justClickedLeft = false;
            return true;
        }

        if (InputHelper.justClickedRight) {
            endAllySelection();
            return false;
        }

        return false;
    }

    /**
     * Obtiene el aliado seleccionado
     */
    public static AbstractPlayer getSelectedAlly() {
        return selectedAlly;
    }

    /**
     * Obtiene el aliado sobre el que está el ratón
     */
    public static AbstractPlayer getHoveredAlly() {
        return hoveredAlly;
    }

    /**
     * Lista de todos los aliados disponibles (copia defensiva)
     */
    public static List<AbstractPlayer> getAvailableAllies() {
        if (!cacheValid) {
            refreshAlliesCache();
        }
        return new ArrayList<>(cachedAllies);
    }

    /**
     * Verifica si está en modo de selección
     */
    public static boolean isSelectingAlly() {
        return isSelecting;
    }

    /**
     * Verifica si Together in Spire está detectado
     */
    public static boolean isTogetherInSpireDetected() {
        if (!tisInitialized) {
            initializeAccessors();
        }
        return tisDetected;
    }

    /**
     * Renderiza indicadores visuales sobre los aliados
     */
    public static void render(SpriteBatch sb) {
        if (!isSelecting || cachedAllies.isEmpty()) {
            return;
        }

        for (AbstractPlayer ally : cachedAllies) {
            Color color = (ally == hoveredAlly) ? SELECTED_COLOR : HIGHLIGHT_COLOR;
            if (ally.hb != null) {
                sb.setColor(color);
            }
        }
    }
}
