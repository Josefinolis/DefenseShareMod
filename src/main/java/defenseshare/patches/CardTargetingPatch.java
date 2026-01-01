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
     * Patch para modificar dinámicamente el target de cartas de defensa
     * Esto permite que requieran selección de objetivo en lugar de aplicarse automáticamente
     */
    @SpirePatch(
        clz = AbstractCard.class,
        method = "update"
    )
    public static class UpdateTargetPatch {

        @SpirePostfixPatch
        public static void Postfix(AbstractCard __instance) {
            if (!DefenseShareMod.isTogetherInSpireLoaded()) {
                return;
            }

            // Solo modificar cartas de defensa que están en la mano del jugador
            if (DefenseCardDetector.isDefenseCard(__instance) &&
                AllyManager.hasAlliesAvailable() &&
                AbstractDungeon.player != null &&
                AbstractDungeon.player.hand != null &&
                AbstractDungeon.player.hand.contains(__instance)) {

                // Guardar el target original si no lo hemos hecho
                if (!originalTargets.containsKey(__instance)) {
                    originalTargets.put(__instance, __instance.target);
                }

                // Cambiar a modo de selección de enemigo (lo usaremos para seleccionar aliados)
                if (__instance.target == AbstractCard.CardTarget.SELF) {
                    __instance.target = AbstractCard.CardTarget.ENEMY;
                }
            } else if (originalTargets.containsKey(__instance)) {
                // Restaurar el target original si la carta ya no está en condiciones
                __instance.target = originalTargets.get(__instance);
                originalTargets.remove(__instance);
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
     * Patch para interceptar el uso de cartas de defensa y establecer el aliado objetivo
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
                !DefenseCardDetector.isDefenseCard(c) ||
                !AllyManager.hasAlliesAvailable()) {
                return SpireReturn.Continue();
            }

            // Verificar si se está usando la carta sobre un aliado
            AbstractPlayer hoveredAlly = getHoveredAlly();
            if (hoveredAlly != null && monster == null) {
                logger.info("Usando carta de defensa " + c.name + " en aliado: " + hoveredAlly.name);

                // Establecer el aliado como objetivo para GainBlockPatch
                defenseshare.patches.GainBlockPatch.setTargetAlly(hoveredAlly);
            }

            // Restaurar el target original de la carta después de usarla
            if (originalTargets.containsKey(c)) {
                AbstractCard.CardTarget originalTarget = originalTargets.get(c);
                // Programar la restauración para después de que se use la carta
                // (esto se hará en el próximo update)
            }

            // Continuar con el uso normal de la carta
            // El GainBlockPatch redirigirá el block si hay un aliado objetivo establecido
            return SpireReturn.Continue();
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance, AbstractCard c, AbstractMonster monster, int energyOnUse) {
            // Limpiar el mapa de targets originales después de usar la carta
            if (originalTargets.containsKey(c)) {
                originalTargets.remove(c);
            }
        }
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
