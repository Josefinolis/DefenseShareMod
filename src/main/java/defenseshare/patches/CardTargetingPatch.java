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

import java.util.HashMap;
import java.util.Map;

/**
 * Patch para permitir que cartas de defensa se apunten a aliados o a uno mismo.
 * Cambia el target a ENEMY para permitir apuntar, luego:
 * - Si apuntas a un aliado Network*, el block va al aliado
 * - Si apuntas a un enemigo normal, el block va a ti mismo
 */
public class CardTargetingPatch {

    // Mapa para guardar el target original de las cartas
    private static final Map<AbstractCard, AbstractCard.CardTarget> originalTargets = new HashMap<>();

    /**
     * Patch para cambiar el target de cartas de defensa cuando hay aliados
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

            if (AbstractDungeon.player == null || AbstractDungeon.player.hand == null) {
                return;
            }

            boolean inHand = AbstractDungeon.player.hand.contains(__instance);
            if (!inHand) {
                // Restaurar target si salió de la mano
                if (originalTargets.containsKey(__instance)) {
                    __instance.target = originalTargets.remove(__instance);
                }
                return;
            }

            if (!DefenseCardDetector.isDefenseCard(__instance)) {
                return;
            }

            if (!AllyManager.hasAlliesAvailable()) {
                if (originalTargets.containsKey(__instance)) {
                    __instance.target = originalTargets.remove(__instance);
                }
                return;
            }

            // Guardar target original y cambiar a ENEMY para poder apuntar
            if (!originalTargets.containsKey(__instance)) {
                originalTargets.put(__instance, __instance.target);
            }

            if (__instance.target == AbstractCard.CardTarget.SELF) {
                __instance.target = AbstractCard.CardTarget.ENEMY;
            }
        }
    }

    /**
     * Patch para detectar el objetivo cuando se usa la carta
     */
    @SpirePatch(
        clz = AbstractPlayer.class,
        method = "useCard"
    )
    public static class UseCardPatch {

        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(AbstractPlayer __instance, AbstractCard c, AbstractMonster monster, int energyOnUse) {
            if (!DefenseShareMod.isTogetherInSpireLoaded() ||
                !DefenseCardDetector.isDefenseCard(c)) {
                return SpireReturn.Continue();
            }

            // Verificar si el monstruo apuntado es realmente un aliado Network*
            if (monster != null && isNetworkAlly(monster)) {
                // Redirigir block al aliado
                GainBlockPatch.setTargetAlly(monster);
            }
            // Si es un enemigo normal o null, el block va al jugador (comportamiento normal)

            return SpireReturn.Continue();
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance, AbstractCard c, AbstractMonster monster, int energyOnUse) {
            originalTargets.remove(c);
            GainBlockPatch.clearTargetAlly();
        }
    }

    /**
     * Verifica si un monstruo es realmente un aliado Network* de TiS
     */
    private static boolean isNetworkAlly(AbstractMonster monster) {
        if (monster == null) return false;
        String className = monster.getClass().getName();
        return className.startsWith("spireTogether.monsters.playerChars.Network");
    }
}
