package defenseshare.patches;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import defenseshare.DefenseShareMod;
import defenseshare.util.AllyManager;
import defenseshare.util.DefenseCardDetector;

import java.util.HashMap;
import java.util.Map;

/**
 * Patch para permitir que cartas de defensa se apunten a aliados.
 * Mantener SHIFT activa el modo aliado - las cartas cambian a ENEMY target.
 */
public class CardTargetingPatch {

    private static final Map<AbstractCard, AbstractCard.CardTarget> originalTargets = new HashMap<>();

    /**
     * Verifica si el modo aliado está activo (Shift presionado)
     */
    public static boolean isAllyModeActive() {
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ||
               Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
    }

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

            // Solo cambiar target si Shift está presionado
            if (isAllyModeActive()) {
                if (!originalTargets.containsKey(__instance)) {
                    originalTargets.put(__instance, __instance.target);
                }
                if (__instance.target == AbstractCard.CardTarget.SELF) {
                    __instance.target = AbstractCard.CardTarget.ENEMY;
                }
            } else {
                // Restaurar target original si Shift no está presionado
                if (originalTargets.containsKey(__instance)) {
                    __instance.target = originalTargets.remove(__instance);
                }
            }
        }
    }

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

            // Solo redirigir si está en modo aliado y apunta a un Network*
            if (isAllyModeActive() && monster != null && isNetworkAlly(monster)) {
                GainBlockPatch.setTargetAlly(monster);
            }

            return SpireReturn.Continue();
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance, AbstractCard c, AbstractMonster monster, int energyOnUse) {
            originalTargets.remove(c);
            GainBlockPatch.clearTargetAlly();
        }
    }

    private static boolean isNetworkAlly(AbstractMonster monster) {
        if (monster == null) return false;
        String className = monster.getClass().getName();
        return className.startsWith("spireTogether.monsters.playerChars.Network");
    }
}
