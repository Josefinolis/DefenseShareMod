package defenseshare.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Manejador de aliados para Together in Spire
 * Detecta y permite seleccionar aliados en partidas cooperativas
 */
public class AllyManager {

    private static final Logger logger = LogManager.getLogger(AllyManager.class.getName());

    // Estado de la selección
    private static boolean isSelecting = false;
    private static List<AbstractPlayer> availableAllies = new ArrayList<>();
    private static AbstractPlayer selectedAlly = null;
    private static AbstractPlayer hoveredAlly = null;

    // Referencia a la clase de Together in Spire (cargada dinámicamente)
    private static Class<?> tisClass = null;
    private static Object tisInstance = null;
    private static boolean tisDetected = false;

    // Configuración visual
    private static final Color HIGHLIGHT_COLOR = new Color(0.3f, 0.8f, 1.0f, 0.5f);
    private static final Color SELECTED_COLOR = new Color(0.2f, 1.0f, 0.2f, 0.7f);

    public static void initialize() {
        logger.info("Inicializando AllyManager...");
        detectTogetherInSpire();
    }

    /**
     * Detecta e inicializa la conexión con Together in Spire
     */
    private static void detectTogetherInSpire() {
        String[] possibleClassNames = {
            "togetherinspire.TogetherInSpire",
            "tis.TogetherInSpire",
            "togetherinspire.TIS",
            "com.togetherinspire.TogetherInSpire"
        };

        for (String className : possibleClassNames) {
            try {
                tisClass = Class.forName(className);
                tisDetected = true;
                logger.info("Together in Spire detectado: " + className);

                // Intentar obtener la instancia singleton si existe
                try {
                    Field instanceField = tisClass.getDeclaredField("instance");
                    instanceField.setAccessible(true);
                    tisInstance = instanceField.get(null);
                } catch (Exception e) {
                    logger.debug("No se encontró campo instance en TiS");
                }

                break;
            } catch (ClassNotFoundException e) {
                // Continuar buscando
            }
        }

        if (!tisDetected) {
            logger.info("Together in Spire no encontrado - el mod funcionará cuando esté instalado");
        }
    }

    /**
     * Verifica si estamos en una partida cooperativa
     */
    public static boolean isInCoopGame() {
        if (!tisDetected) {
            return false;
        }

        try {
            // Intentar llamar al método que indica si estamos en coop
            String[] methodNames = {"isInMultiplayer", "isCoopActive", "isInGame", "isConnected"};

            for (String methodName : methodNames) {
                try {
                    Method method = tisClass.getMethod(methodName);
                    Object result = method.invoke(tisInstance);
                    if (result instanceof Boolean && (Boolean) result) {
                        return true;
                    }
                } catch (NoSuchMethodException e) {
                    // Probar siguiente método
                }
            }
        } catch (Exception e) {
            logger.debug("Error verificando estado coop: " + e.getMessage());
        }

        // Fallback: verificar si hay más de un jugador de alguna manera
        return checkForMultiplePlayers();
    }

    /**
     * Verifica si hay múltiples jugadores (método alternativo)
     */
    private static boolean checkForMultiplePlayers() {
        // Esto es un fallback - intentar detectar otros jugadores de otras formas
        return availableAllies.size() > 0;
    }

    /**
     * Verifica si hay aliados disponibles para recibir defensa
     */
    public static boolean hasAlliesAvailable() {
        updateAlliesList();
        boolean result = !availableAllies.isEmpty();
        logger.info("[DEBUG] hasAlliesAvailable() = " + result + " (aliados: " + availableAllies.size() + ")");
        return result;
    }

    /**
     * Actualiza la lista de aliados disponibles desde Together in Spire
     */
    private static void updateAlliesList() {
        availableAllies.clear();

        if (!tisDetected) {
            logger.info("[DEBUG] Together in Spire NO detectado en AllyManager");
            return;
        }

        logger.info("[DEBUG] Together in Spire detectado, buscando aliados...");

        try {
            // Intentar obtener la lista de jugadores de TiS
            String[] fieldNames = {"players", "otherPlayers", "allies", "connectedPlayers", "coopPlayers"};

            for (String fieldName : fieldNames) {
                try {
                    Field field = tisClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object playersObj = field.get(tisInstance);

                    if (playersObj instanceof List<?>) {
                        List<?> players = (List<?>) playersObj;
                        for (Object player : players) {
                            if (player instanceof AbstractPlayer) {
                                AbstractPlayer p = (AbstractPlayer) player;
                                // No incluir al jugador actual
                                if (p != AbstractDungeon.player && p.currentHealth > 0) {
                                    availableAllies.add(p);
                                }
                            }
                        }
                        if (!availableAllies.isEmpty()) {
                            break;
                        }
                    }
                } catch (NoSuchFieldException e) {
                    // Probar siguiente campo
                }
            }

            // Método alternativo: buscar mediante métodos getter
            if (availableAllies.isEmpty()) {
                String[] methodNames = {"getPlayers", "getOtherPlayers", "getAllies", "getConnectedPlayers"};

                for (String methodName : methodNames) {
                    try {
                        Method method = tisClass.getMethod(methodName);
                        Object result = method.invoke(tisInstance);

                        if (result instanceof List<?>) {
                            List<?> players = (List<?>) result;
                            for (Object player : players) {
                                if (player instanceof AbstractPlayer) {
                                    AbstractPlayer p = (AbstractPlayer) player;
                                    if (p != AbstractDungeon.player && p.currentHealth > 0) {
                                        availableAllies.add(p);
                                    }
                                }
                            }
                            if (!availableAllies.isEmpty()) {
                                break;
                            }
                        }
                    } catch (NoSuchMethodException e) {
                        // Probar siguiente método
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error obteniendo lista de aliados: " + e.getMessage());
        }

        logger.debug("Aliados disponibles: " + availableAllies.size());
    }

    /**
     * Inicia el modo de selección de aliado
     */
    public static void startAllySelection() {
        updateAlliesList();
        if (!availableAllies.isEmpty()) {
            isSelecting = true;
            selectedAlly = null;
            hoveredAlly = null;
            logger.info("Iniciando selección de aliado. Aliados disponibles: " + availableAllies.size());
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
     * Retorna true si se seleccionó un aliado
     */
    public static boolean updateAllySelection() {
        if (!isSelecting || availableAllies.isEmpty()) {
            return false;
        }

        hoveredAlly = null;

        // Detectar sobre qué aliado está el ratón
        float mouseX = InputHelper.mX;
        float mouseY = InputHelper.mY;

        for (AbstractPlayer ally : availableAllies) {
            if (ally.hb != null && ally.hb.hovered) {
                hoveredAlly = ally;
                break;
            }

            // Verificación alternativa de posición
            float allyX = ally.drawX;
            float allyY = ally.drawY;
            float hitboxWidth = 150 * Settings.scale;
            float hitboxHeight = 200 * Settings.scale;

            if (mouseX >= allyX - hitboxWidth / 2 && mouseX <= allyX + hitboxWidth / 2 &&
                mouseY >= allyY - hitboxHeight / 2 && mouseY <= allyY + hitboxHeight / 2) {
                hoveredAlly = ally;
                break;
            }
        }

        // Click para seleccionar
        if (hoveredAlly != null && InputHelper.justClickedLeft) {
            selectedAlly = hoveredAlly;
            logger.info("Aliado seleccionado: " + selectedAlly.name);
            InputHelper.justClickedLeft = false; // Consumir el click
            return true;
        }

        // Click derecho o Escape para cancelar
        if (InputHelper.justClickedRight || Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            logger.info("Selección de aliado cancelada");
            endAllySelection();
            selectedAlly = null;
            return false;
        }

        return false;
    }

    /**
     * Obtiene el aliado seleccionado (null si no hay ninguno)
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
     * Lista de todos los aliados disponibles
     */
    public static List<AbstractPlayer> getAvailableAllies() {
        return new ArrayList<>(availableAllies);
    }

    /**
     * Verifica si está en modo de selección
     */
    public static boolean isSelectingAlly() {
        return isSelecting;
    }

    /**
     * Renderiza indicadores visuales sobre los aliados seleccionables
     */
    public static void render(SpriteBatch sb) {
        if (!isSelecting || availableAllies.isEmpty()) {
            return;
        }

        // Dibujar highlight sobre aliados disponibles
        for (AbstractPlayer ally : availableAllies) {
            Color color = (ally == hoveredAlly) ? SELECTED_COLOR : HIGHLIGHT_COLOR;

            // Aquí se dibujaría un indicador visual
            // El renderizado específico depende de cómo Together in Spire
            // renderiza a los otros jugadores

            // Por ahora, usamos el hitbox del jugador para mostrar un outline
            if (ally.hb != null) {
                // Render básico de hitbox highlight
                sb.setColor(color);
                // El renderizado específico se implementaría aquí
            }
        }
    }
}
