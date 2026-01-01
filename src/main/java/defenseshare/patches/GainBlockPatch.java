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
            if (targetAllyForNextBlock != null &&
                target == AbstractDungeon.player &&
                DefenseShareMod.isTogetherInSpireLoaded()) {

                try {
                    // Usar reflection para cambiar el target de la acción
                    java.lang.reflect.Field targetField = GainBlockAction.class.getDeclaredField("target");
                    targetField.setAccessible(true);
                    targetField.set(__instance, targetAllyForNextBlock);

                    logger.info("GainBlockAction redirigido a aliado: " + targetAllyForNextBlock.name +
                               " por " + amount + " de block");

                } catch (Exception e) {
                    logger.error("Error redirigiendo GainBlockAction: " + e.getMessage());
                }

                // Limpiar el aliado objetivo después de usarlo
                clearTargetAlly();
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
            if (targetAllyForNextBlock != null &&
                target == AbstractDungeon.player &&
                DefenseShareMod.isTogetherInSpireLoaded()) {

                try {
                    java.lang.reflect.Field targetField = GainBlockAction.class.getDeclaredField("target");
                    targetField.setAccessible(true);
                    targetField.set(__instance, targetAllyForNextBlock);

                    logger.info("GainBlockAction (3-param) redirigido a aliado: " +
                               targetAllyForNextBlock.name + " por " + amount + " de block");

                } catch (Exception e) {
                    logger.error("Error redirigiendo GainBlockAction (3-param): " + e.getMessage());
                }

                clearTargetAlly();
            }
        }
    }
}
