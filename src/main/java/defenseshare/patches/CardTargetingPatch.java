package defenseshare.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.core.AbstractCreature;

import defenseshare.DefenseShareMod;
import defenseshare.util.AllyManager;
import defenseshare.util.DefenseCardDetector;

import javassist.CtBehavior;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Patch que modifica el sistema de targeting de cartas
 * para permitir apuntar cartas de defensa a aliados
 */
public class CardTargetingPatch {

    private static final Logger logger = LogManager.getLogger(CardTargetingPatch.class.getName());

    // Mapa para guardar el target original de las cartas
    private static final Map<AbstractCard, AbstractCard.CardTarget> originalTargets = new HashMap<>();

    /**
     * Patch para cambiar el target de cartas de defensa cuando hay aliados disponibles
     */
    @SpirePatch(
        clz = AbstractCard.class,
        method = "update"
    )
    public static class UpdateTargetPatch {

        @SpirePostfixPatch
        public static void Postfix(AbstractCard __instance) {
            // DEBUG: Log cada vez que se ejecuta el patch
            if (__instance.name.contains("Defend")) {
                logger.info("[DEBUG] UpdateTargetPatch ejecutado para: " + __instance.name);
            }

            if (!DefenseShareMod.isTogetherInSpireLoaded()) {
                if (__instance.name.contains("Defend")) {
                    logger.info("[DEBUG] Together in Spire NO está cargado");
                }
                return;
            }

            // Verificar si estamos en combate
            if (AbstractDungeon.player == null || AbstractDungeon.player.hand == null) {
                return;
            }

            boolean isDefense = DefenseCardDetector.isDefenseCard(__instance);
            boolean hasAllies = AllyManager.hasAlliesAvailable();
            boolean inHand = AbstractDungeon.player.hand.contains(__instance);

            // DEBUG: Log para cartas de defensa
            if (__instance.name.contains("Defend")) {
                logger.info("[DEBUG] " + __instance.name + " - isDefense: " + isDefense + ", hasAllies: " + hasAllies + ", inHand: " + inHand);
            }

            // Solo modificar cartas de defensa que están en la mano
            if (isDefense && hasAllies && inHand) {

                // Guardar el target original
                if (!originalTargets.containsKey(__instance)) {
                    originalTargets.put(__instance, __instance.target);
                    logger.info("[DEBUG] Guardando target original de " + __instance.name + ": " + __instance.target);
                }

                // Cambiar a ENEMY para que funcione como cartas de ataque
                if (__instance.target == AbstractCard.CardTarget.SELF) {
                    __instance.target = AbstractCard.CardTarget.ENEMY;
                    logger.info("[DEBUG] ★★★ Cambiando target de " + __instance.name + " a ENEMY ★★★");
                }
            } else if (originalTargets.containsKey(__instance)) {
                // Restaurar el target original si ya no está en la mano
                if (!AbstractDungeon.player.hand.contains(__instance)) {
                    __instance.target = originalTargets.get(__instance);
                    originalTargets.remove(__instance);
                    logger.info("[DEBUG] Restaurando target original de " + __instance.name);
                }
            }
        }
    }

    /**
     * Patch para detectar cuando se usa una carta de defensa en un aliado
     */
    @SpirePatch(
        clz = AbstractPlayer.class,
        method = "useCard"
    )
    public static class UseCardPatch {

        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(AbstractPlayer __instance, AbstractCard c, AbstractMonster monster, int energyOnUse) {
            // Solo procesar cartas de defensa
            if (!DefenseShareMod.isTogetherInSpireLoaded() ||
                !DefenseCardDetector.isDefenseCard(c)) {
                return SpireReturn.Continue();
            }

            // Si se está usando sobre un monstruo que es realmente un aliado (TiS hace esto)
            // o si monster es null pero hay un aliado seleccionado
            if (monster != null || AllyManager.hasAlliesAvailable()) {
                // Buscar el aliado sobre el que está el mouse
                AbstractPlayer hoveredAlly = getHoveredAlly();
                if (hoveredAlly != null) {
                    logger.info("Carta de defensa " + c.name + " usada en aliado: " + hoveredAlly.name);

                    // Establecer el aliado como objetivo para el GainBlockPatch
                    defenseshare.patches.GainBlockPatch.setTargetAlly(hoveredAlly);
                }
            }

            return SpireReturn.Continue();
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance, AbstractCard c, AbstractMonster monster, int energyOnUse) {
            // Limpiar el target original después de usar la carta
            if (originalTargets.containsKey(c)) {
                originalTargets.remove(c);
            }
        }
    }

    /**
     * Método auxiliar para detectar si el mouse está sobre un aliado
     */
    private static AbstractPlayer getHoveredAlly() {
        for (AbstractPlayer ally : AllyManager.getAvailableAllies()) {
            if (ally.hb != null && ally.hb.hovered) {
                return ally;
            }
        }
        return null;
    }

    /**
     * Locator para encontrar el punto de inserción en métodos complejos
     */
    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher matcher = new Matcher.MethodCallMatcher(AbstractPlayer.class, "gainBlock");
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }
}
