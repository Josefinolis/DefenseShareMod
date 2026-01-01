package defenseshare.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import defenseshare.DefenseShareMod;
import defenseshare.util.AllyManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patch para interceptar y modificar GainBlockAction
 * Permite redirigir el block a aliados cuando sea necesario
 */
public class GainBlockPatch {

    private static final Logger logger = LogManager.getLogger(GainBlockPatch.class.getName());

    // Variable para rastrear si el próximo GainBlockAction debe ir a un aliado
    private static AbstractPlayer targetAllyForNextBlock = null;

    /**
     * Establece el aliado que debe recibir el próximo block
     */
    public static void setTargetAlly(AbstractPlayer ally) {
        targetAllyForNextBlock = ally;
    }

    /**
     * Limpia el aliado objetivo
     */
    public static void clearTargetAlly() {
        targetAllyForNextBlock = null;
    }

    /**
     * Patch del constructor de GainBlockAction para redirigir a aliados
     */
    @SpirePatch(
        clz = GainBlockAction.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {AbstractCreature.class, int.class}
    )
    public static class GainBlockConstructorPatch {

        @SpirePostfixPatch
        public static void Postfix(GainBlockAction __instance, AbstractCreature target, int amount) {
            // Si hay un aliado objetivo establecido y el target es el jugador actual
            if (targetAllyForNextBlock != null && DefenseShareMod.isTogetherInSpireLoaded()) {

                logger.info("GainBlockAction detectado - Target original: " +
                           (target != null ? target.name : "null") +
                           ", Amount: " + amount +
                           ", Aliado objetivo: " + targetAllyForNextBlock.name);

                // Redirigir solo si el target es el jugador actual
                if (target == AbstractDungeon.player) {
                    try {
                        // Usar reflection para cambiar el target de la acción
                        java.lang.reflect.Field targetField = GainBlockAction.class.getDeclaredField("target");
                        targetField.setAccessible(true);
                        targetField.set(__instance, targetAllyForNextBlock);

                        logger.info("✓ Block redirigido exitosamente a " + targetAllyForNextBlock.name);

                    } catch (NoSuchFieldException e) {
                        logger.error("Error: Campo 'target' no encontrado en GainBlockAction");
                        logger.error("Campos disponibles: ");
                        for (java.lang.reflect.Field f : GainBlockAction.class.getDeclaredFields()) {
                            logger.error("  - " + f.getName());
                        }
                    } catch (Exception e) {
                        logger.error("Error redirigiendo GainBlockAction: " + e.getMessage());
                        e.printStackTrace();
                    }

                    // Limpiar el aliado objetivo después de usarlo
                    clearTargetAlly();
                } else {
                    logger.info("Target no es el jugador actual, no se redirige");
                }
            }
        }
    }

    /**
     * Patch alternativo para el constructor con 3 parámetros
     */
    @SpirePatch(
        clz = GainBlockAction.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {AbstractCreature.class, AbstractCreature.class, int.class}
    )
    public static class GainBlockConstructor3Patch {

        @SpirePostfixPatch
        public static void Postfix(GainBlockAction __instance, AbstractCreature target, AbstractCreature source, int amount) {
            if (targetAllyForNextBlock != null && DefenseShareMod.isTogetherInSpireLoaded()) {

                logger.info("GainBlockAction (3-param) detectado - Target: " +
                           (target != null ? target.name : "null") +
                           ", Source: " + (source != null ? source.name : "null") +
                           ", Amount: " + amount +
                           ", Aliado objetivo: " + targetAllyForNextBlock.name);

                // Redirigir solo si el target es el jugador actual
                if (target == AbstractDungeon.player) {
                    try {
                        java.lang.reflect.Field targetField = GainBlockAction.class.getDeclaredField("target");
                        targetField.setAccessible(true);
                        targetField.set(__instance, targetAllyForNextBlock);

                        logger.info("✓ Block (3-param) redirigido exitosamente a " + targetAllyForNextBlock.name);

                    } catch (NoSuchFieldException e) {
                        logger.error("Error: Campo 'target' no encontrado en GainBlockAction (3-param)");
                        logger.error("Campos disponibles: ");
                        for (java.lang.reflect.Field f : GainBlockAction.class.getDeclaredFields()) {
                            logger.error("  - " + f.getName());
                        }
                    } catch (Exception e) {
                        logger.error("Error redirigiendo GainBlockAction (3-param): " + e.getMessage());
                        e.printStackTrace();
                    }

                    clearTargetAlly();
                } else {
                    logger.info("Target no es el jugador actual, no se redirige (3-param)");
                }
            }
        }
    }
}
