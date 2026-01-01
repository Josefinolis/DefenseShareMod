package defenseshare.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import defenseshare.DefenseShareMod;
import defenseshare.util.AllyManager;
import defenseshare.util.DefenseCardDetector;

import javassist.CtBehavior;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patch que modifica el sistema de targeting de cartas
 * para permitir apuntar cartas de defensa a aliados
 */
public class CardTargetingPatch {

    private static final Logger logger = LogManager.getLogger(CardTargetingPatch.class.getName());

    /**
     * Patch para interceptar cuando una carta está siendo seleccionada/hovereada
     */
    @SpirePatch(
        clz = AbstractCard.class,
        method = "canUse"
    )
    public static class CanUsePatch {

        @SpirePostfixPatch
        public static boolean Postfix(boolean __result, AbstractCard __instance, AbstractPlayer p, AbstractMonster m) {
            // Si la carta puede usarse normalmente, verificar si también puede usarse en aliados
            if (__result && DefenseShareMod.isTogetherInSpireLoaded()) {
                if (DefenseCardDetector.isDefenseCard(__instance)) {
                    // Las cartas de defensa siempre pueden usarse si hay aliados
                    if (AllyManager.hasAlliesAvailable()) {
                        return true;
                    }
                }
            }
            return __result;
        }
    }

    /**
     * Patch para modificar el targeting visual de cartas de defensa
     */
    @SpirePatch(
        clz = AbstractCard.class,
        method = "calculateCardDamage"
    )
    public static class CalculateCardDamagePatch {

        @SpirePostfixPatch
        public static void Postfix(AbstractCard __instance, AbstractMonster mo) {
            // Si es una carta de defensa y estamos en coop, recalcular block para aliados
            if (DefenseShareMod.isTogetherInSpireLoaded() && DefenseCardDetector.isDefenseCard(__instance)) {
                // El block ya está calculado, pero podemos añadir modificadores aquí si es necesario
                // Por ejemplo, si Together in Spire tiene bonificaciones de equipo
            }
        }
    }

    /**
     * Patch para interceptar el uso de cartas
     */
    @SpirePatch(
        clz = AbstractPlayer.class,
        method = "useCard"
    )
    public static class UseCardPatch {

        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(AbstractPlayer __instance, AbstractCard c, AbstractMonster monster, int energyOnUse) {
            // Verificar si es una carta de defensa que debe ir a un aliado
            if (DefenseShareMod.isTogetherInSpireLoaded() &&
                DefenseCardDetector.isDefenseCard(c) &&
                AllyManager.isSelectingAlly()) {

                AbstractPlayer selectedAlly = AllyManager.getSelectedAlly();

                if (selectedAlly != null) {
                    // Redirigir la defensa al aliado seleccionado
                    logger.info("Redirigiendo defensa de carta " + c.name + " a aliado " + selectedAlly.name);

                    // La lógica de aplicar defensa al aliado se maneja en DefenseShareMod
                    // Aquí solo registramos que se ha seleccionado un aliado

                    // Permitir que la carta se use normalmente (el block se aplicará al aliado en post-use)
                }
            }

            // Continuar con el uso normal de la carta
            return SpireReturn.Continue();
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
