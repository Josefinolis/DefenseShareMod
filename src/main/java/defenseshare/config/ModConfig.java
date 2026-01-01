package defenseshare.config;

import basemod.ModLabel;
import basemod.ModPanel;
import basemod.BaseMod;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;

import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Configuración del mod Defense Share
 */
public class ModConfig {

    private static final Logger logger = LogManager.getLogger(ModConfig.class.getName());

    private static SpireConfig config;
    private static final String CONFIG_NAME = "DefenseShareConfig";

    // Propiedades de configuración
    public static boolean REQUIRE_HOLD_KEY = false;        // Requiere mantener tecla para compartir
    public static boolean SHOW_SHARE_INDICATOR = true;      // Mostrar indicador [ALLY] en cartas
    public static boolean AUTO_SELECT_LOWEST_HP = false;    // Auto-seleccionar aliado con menos HP
    public static String SHARE_HOTKEY = "SHIFT";            // Tecla para activar compartir

    // Valores por defecto
    private static final String DEFAULT_REQUIRE_HOLD_KEY = "false";
    private static final String DEFAULT_SHOW_SHARE_INDICATOR = "true";
    private static final String DEFAULT_AUTO_SELECT_LOWEST_HP = "false";
    private static final String DEFAULT_SHARE_HOTKEY = "SHIFT";

    public static void initialize() {
        logger.info("Inicializando configuración del mod...");

        Properties defaults = new Properties();
        defaults.setProperty("requireHoldKey", DEFAULT_REQUIRE_HOLD_KEY);
        defaults.setProperty("showShareIndicator", DEFAULT_SHOW_SHARE_INDICATOR);
        defaults.setProperty("autoSelectLowestHP", DEFAULT_AUTO_SELECT_LOWEST_HP);
        defaults.setProperty("shareHotkey", DEFAULT_SHARE_HOTKEY);

        try {
            config = new SpireConfig("DefenseShareMod", CONFIG_NAME, defaults);
            loadConfig();
        } catch (IOException e) {
            logger.error("Error cargando configuración: " + e.getMessage());
            // Usar valores por defecto
        }
    }

    private static void loadConfig() {
        REQUIRE_HOLD_KEY = config.getBool("requireHoldKey");
        SHOW_SHARE_INDICATOR = config.getBool("showShareIndicator");
        AUTO_SELECT_LOWEST_HP = config.getBool("autoSelectLowestHP");
        SHARE_HOTKEY = config.getString("shareHotkey");

        logger.info("Configuración cargada:");
        logger.info("  - requireHoldKey: " + REQUIRE_HOLD_KEY);
        logger.info("  - showShareIndicator: " + SHOW_SHARE_INDICATOR);
        logger.info("  - autoSelectLowestHP: " + AUTO_SELECT_LOWEST_HP);
        logger.info("  - shareHotkey: " + SHARE_HOTKEY);
    }

    public static void saveConfig() {
        try {
            config.setBool("requireHoldKey", REQUIRE_HOLD_KEY);
            config.setBool("showShareIndicator", SHOW_SHARE_INDICATOR);
            config.setBool("autoSelectLowestHP", AUTO_SELECT_LOWEST_HP);
            config.setString("shareHotkey", SHARE_HOTKEY);
            config.save();
            logger.info("Configuración guardada");
        } catch (IOException e) {
            logger.error("Error guardando configuración: " + e.getMessage());
        }
    }

    /**
     * Crea el panel de configuración del mod para BaseMod
     */
    public static ModPanel createConfigPanel() {
        // Crear textura de fondo (placeholder)
        Texture badgeTexture = ImageMaster.loadImage("images/badge.png");

        ModPanel settingsPanel = new ModPanel();

        // Título
        ModLabel titleLabel = new ModLabel(
            "Defense Share - Configuración",
            400.0f,
            750.0f,
            Settings.CREAM_COLOR,
            FontHelper.charDescFont,
            settingsPanel,
            (me) -> {}
        );
        settingsPanel.addUIElement(titleLabel);

        // Descripción
        ModLabel descLabel = new ModLabel(
            "Permite compartir cartas de defensa con aliados en Together in Spire",
            400.0f,
            700.0f,
            Color.GRAY,
            FontHelper.tipBodyFont,
            settingsPanel,
            (me) -> {}
        );
        settingsPanel.addUIElement(descLabel);

        // Instrucciones
        ModLabel instructionsLabel = new ModLabel(
            "Al jugar una carta de defensa, haz click en un aliado para enviarle el bloqueo",
            400.0f,
            650.0f,
            Color.GRAY,
            FontHelper.tipBodyFont,
            settingsPanel,
            (me) -> {}
        );
        settingsPanel.addUIElement(instructionsLabel);

        return settingsPanel;
    }
}
