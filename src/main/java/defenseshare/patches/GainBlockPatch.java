package defenseshare.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import defenseshare.DefenseShareMod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Patch para interceptar y modificar GainBlockAction
 * Permite redirigir el block a aliados cuando sea necesario
 */
public class GainBlockPatch {

    private static final Logger logger = LogManager.getLogger(GainBlockPatch.class.getName());

    // Variable para rastrear si el próximo GainBlockAction debe ir a un aliado
    private static AbstractCreature targetAllyForNextBlock = null;

    // Field pre-cargado para evitar reflection en cada uso
    private static Field targetField = null;
    private static boolean fieldInitialized = false;

    /**
     * Inicializa el field una sola vez
     */
    private static void initializeField() {
        if (fieldInitialized) return;
        fieldInitialized = true;

        try {
            targetField = GainBlockAction.class.getDeclaredField("target");
            targetField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            logger.error("Campo 'target' no encontrado en GainBlockAction");
        }
    }

    /**
     * Establece el aliado que debe recibir el próximo block
     */
    public static void setTargetAlly(AbstractCreature ally) {
        targetAllyForNextBlock = ally;
    }

    /**
     * Limpia el aliado objetivo
     */
    public static void clearTargetAlly() {
        targetAllyForNextBlock = null;
    }

    /**
     * Redirige el target de una GainBlockAction al aliado
     */
    private static void redirectToAlly(GainBlockAction action, AbstractCreature originalTarget) {
        if (targetAllyForNextBlock == null || !DefenseShareMod.isTogetherInSpireLoaded()) {
            return;
        }

        // Solo redirigir si el target es el jugador actual
        if (originalTarget != AbstractDungeon.player) {
            return;
        }

        initializeField();

        if (targetField == null) {
            return;
        }

        try {
            targetField.set(action, targetAllyForNextBlock);
            logger.info("Block redirigido a " + targetAllyForNextBlock.name);
        } catch (Exception e) {
            logger.error("Error redirigiendo block: " + e.getMessage());
        }

        clearTargetAlly();
    }

    /**
     * Patch del constructor de GainBlockAction (2 params)
     */
    @SpirePatch(
        clz = GainBlockAction.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {AbstractCreature.class, int.class}
    )
    public static class GainBlockConstructorPatch {

        @SpirePostfixPatch
        public static void Postfix(GainBlockAction __instance, AbstractCreature target, int amount) {
            redirectToAlly(__instance, target);
        }
    }

    /**
     * Patch del constructor de GainBlockAction (3 params)
     */
    @SpirePatch(
        clz = GainBlockAction.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {AbstractCreature.class, AbstractCreature.class, int.class}
    )
    public static class GainBlockConstructor3Patch {

        @SpirePostfixPatch
        public static void Postfix(GainBlockAction __instance, AbstractCreature target, AbstractCreature source, int amount) {
            redirectToAlly(__instance, target);
        }
    }
}
