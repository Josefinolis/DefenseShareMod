package defenseshare.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import defenseshare.DefenseShareMod;
import defenseshare.util.AllyManager;
import defenseshare.util.DefenseCardDetector;

import com.megacrit.cardcrawl.core.AbstractCreature;
import javassist.CtBehavior;

import java.util.HashMap;
import java.util.Map;

/**
 * Patch que modifica el sistema de targeting de cartas
 * para permitir apuntar cartas de defensa a aliados
 *
 * OPTIMIZADO: Sin logging excesivo
 */
public class CardTargetingPatch {

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
            // Early exit si TiS no está cargado
            if (!DefenseShareMod.isTogetherInSpireLoaded()) {
                return;
            }

            // Verificar si estamos en combate
            if (AbstractDungeon.player == null || AbstractDungeon.player.hand == null) {
                return;
            }

            // Solo procesar cartas en la mano
            boolean inHand = AbstractDungeon.player.hand.contains(__instance);
            if (!inHand) {
                // Restaurar target original si la carta salió de la mano
                if (originalTargets.containsKey(__instance)) {
                    __instance.target = originalTargets.remove(__instance);
                }
                return;
            }

            // Solo procesar cartas de defensa
            if (!DefenseCardDetector.isDefenseCard(__instance)) {
                return;
            }

            // Verificar si hay aliados (usa cache, muy rápido)
            if (!AllyManager.hasAlliesAvailable()) {
                // Restaurar target si ya no hay aliados
                if (originalTargets.containsKey(__instance)) {
                    __instance.target = originalTargets.remove(__instance);
                }
                return;
            }

            // Guardar target original y cambiar a ENEMY
            if (!originalTargets.containsKey(__instance)) {
                originalTargets.put(__instance, __instance.target);
            }

            if (__instance.target == AbstractCard.CardTarget.SELF) {
                __instance.target = AbstractCard.CardTarget.ENEMY;
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
            // Solo procesar cartas de defensa con TiS activo
            if (!DefenseShareMod.isTogetherInSpireLoaded() ||
                !DefenseCardDetector.isDefenseCard(c)) {
                return SpireReturn.Continue();
            }

            // Buscar el aliado sobre el que está el mouse
            AbstractCreature hoveredAlly = getHoveredAlly();
            if (hoveredAlly != null) {
                defenseshare.patches.GainBlockPatch.setTargetAlly(hoveredAlly);
            }

            return SpireReturn.Continue();
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance, AbstractCard c, AbstractMonster monster, int energyOnUse) {
            originalTargets.remove(c);
        }
    }

    /**
     * Método auxiliar para detectar si el mouse está sobre un aliado
     */
    private static AbstractCreature getHoveredAlly() {
        for (AbstractCreature ally : AllyManager.getAvailableAllies()) {
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
