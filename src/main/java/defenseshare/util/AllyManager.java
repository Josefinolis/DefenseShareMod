package defenseshare.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manejador de aliados para Together in Spire
 * Detecta y permite seleccionar aliados en partidas cooperativas
 */
public class AllyManager {

    private static final Logger logger = LogManager.getLogger(AllyManager.class.getName());

    // Estado de la selección
    private static boolean isSelecting = false;
    private static AbstractCreature selectedAlly = null;
    private static AbstractCreature hoveredAlly = null;

    // Cache de aliados - usa AbstractCreature para soportar Network* de TiS
    private static List<AbstractCreature> cachedAllies = new ArrayList<>();
    private static boolean cacheValid = false;

    // Detección de Together in Spire
    private static boolean tisDetected = false;
    private static boolean tisInitialized = false;

    // Accessors pre-compilados
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

    private static void initializeAccessors() {
        if (accessorsInitialized) return;
        accessorsInitialized = true;

        try {
            Class<?> tisClass = Class.forName("spireTogether.SpireTogetherMod");
            tisDetected = true;
            tisInitialized = true;
            logger.info("Together in Spire detectado");

            try {
                allCharacterEntitiesField = tisClass.getDeclaredField("allCharacterEntities");
                allCharacterEntitiesField.setAccessible(true);
                logger.info("Campo allCharacterEntities encontrado");
            } catch (NoSuchFieldException e) {
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

    public static boolean hasAlliesAvailable() {
        if (!tisDetected) return false;
        if (cacheValid) return !cachedAllies.isEmpty();
        refreshAlliesCache();
        return !cachedAllies.isEmpty();
    }

    public static void invalidateCache() {
        cacheValid = false;
    }

    public static void refreshAlliesCache() {
        cachedAllies.clear();

        if (!tisDetected || AbstractDungeon.player == null) {
            cacheValid = true;
            return;
        }

        try {
            if (allCharacterEntitiesField != null) {
                Object value = allCharacterEntitiesField.get(null);
                if (value instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) value;
                    logger.info("allCharacterEntities size: " + map.size());
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        Object ally = entry.getValue();
                        if (ally instanceof AbstractCreature) {
                            AbstractCreature creature = (AbstractCreature) ally;
                            // Filtrar: no el jugador actual, con vida, y del paquete Network*
                            if (creature != AbstractDungeon.player &&
                                creature.currentHealth > 0 &&
                                isNetworkPlayer(creature)) {
                                cachedAllies.add(creature);
                                logger.info("Aliado añadido: " + creature.name + " (" + creature.getClass().getSimpleName() + ")");
                            }
                        }
                    }
                }
            }

            if (cachedAllies.isEmpty() && getPlayersMethod != null) {
                Object result = getPlayersMethod.invoke(null);
                extractCreaturesFromObject(result);
            }

        } catch (Exception e) {
            logger.error("Error refrescando cache: " + e.getMessage());
        }

        logger.info("Cache refrescado. Aliados encontrados: " + cachedAllies.size());
        cacheValid = true;
    }

    /**
     * Verifica si una criatura es un jugador de red de TiS
     */
    private static boolean isNetworkPlayer(AbstractCreature creature) {
        String className = creature.getClass().getName();
        return className.startsWith("spireTogether.monsters.playerChars.Network");
    }

    private static void extractCreaturesFromObject(Object obj) {
        if (obj == null) return;
        try {
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                for (Object value : map.values()) {
                    addCreatureIfValid(value);
                }
            } else if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                for (Object item : list) {
                    addCreatureIfValid(item);
                }
            }
        } catch (Exception ignored) {}
    }

    private static void addCreatureIfValid(Object obj) {
        if (obj instanceof AbstractCreature) {
            AbstractCreature c = (AbstractCreature) obj;
            if (c != AbstractDungeon.player && c.currentHealth > 0 && isNetworkPlayer(c)) {
                cachedAllies.add(c);
            }
        }
    }

    public static void startAllySelection() {
        refreshAlliesCache();
        if (!cachedAllies.isEmpty()) {
            isSelecting = true;
            selectedAlly = null;
            hoveredAlly = null;
        }
    }

    public static void endAllySelection() {
        isSelecting = false;
        selectedAlly = null;
        hoveredAlly = null;
    }

    public static boolean updateAllySelection() {
        if (!isSelecting || cachedAllies.isEmpty()) return false;

        hoveredAlly = null;
        float mouseX = InputHelper.mX;
        float mouseY = InputHelper.mY;

        for (AbstractCreature ally : cachedAllies) {
            if (ally.hb != null && ally.hb.hovered) {
                hoveredAlly = ally;
                break;
            }

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

    public static AbstractCreature getSelectedAlly() {
        return selectedAlly;
    }

    public static AbstractCreature getHoveredAlly() {
        return hoveredAlly;
    }

    public static List<AbstractCreature> getAvailableAllies() {
        if (!cacheValid) refreshAlliesCache();
        return new ArrayList<>(cachedAllies);
    }

    public static boolean isSelectingAlly() {
        return isSelecting;
    }

    public static boolean isTogetherInSpireDetected() {
        if (!tisInitialized) initializeAccessors();
        return tisDetected;
    }

    public static void render(SpriteBatch sb) {
        if (!isSelecting || cachedAllies.isEmpty()) return;

        for (AbstractCreature ally : cachedAllies) {
            Color color = (ally == hoveredAlly) ? SELECTED_COLOR : HIGHLIGHT_COLOR;
            if (ally.hb != null) {
                sb.setColor(color);
            }
        }
    }
}
