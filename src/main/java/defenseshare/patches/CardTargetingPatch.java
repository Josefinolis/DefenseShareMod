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

/**
 * Patch para permitir que cartas de defensa se usen sobre aliados
 * Las cartas mantienen su comportamiento normal (SELF), pero si el cursor
 * está sobre un aliado al usarlas, el block se redirige a ese aliado.
 */
public class CardTargetingPatch {

    /**
     * Patch para detectar cuando se usa una carta de defensa
     * Si el cursor está sobre un aliado, redirigir el block
     */
    @SpirePatch(
        clz = AbstractPlayer.class,
        method = "useCard"
    )
    public static class UseCardPatch {

        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(AbstractPlayer __instance, AbstractCard c, AbstractMonster monster, int energyOnUse) {
            // Solo procesar cartas de defensa con TiS activo
            if (!DefenseShareMod.isTogetherInSpireLoaded() ||
                !DefenseCardDetector.isDefenseCard(c)) {
                return SpireReturn.Continue();
            }

            // Buscar si hay un aliado bajo el cursor
            AbstractCreature hoveredAlly = getHoveredAlly();
            if (hoveredAlly != null) {
                // Redirigir el block a este aliado
                GainBlockPatch.setTargetAlly(hoveredAlly);
            }
            // Si no hay aliado bajo el cursor, el block va al jugador normalmente

            return SpireReturn.Continue();
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance, AbstractCard c, AbstractMonster monster, int energyOnUse) {
            // Limpiar después de usar la carta
            if (DefenseCardDetector.isDefenseCard(c)) {
                GainBlockPatch.clearTargetAlly();
            }
        }
    }

    /**
     * Detecta si el cursor está sobre un aliado
     */
    private static AbstractCreature getHoveredAlly() {
        for (AbstractCreature ally : AllyManager.getAvailableAllies()) {
            if (ally.hb != null && ally.hb.hovered) {
                return ally;
            }
        }
        return null;
    }
}
