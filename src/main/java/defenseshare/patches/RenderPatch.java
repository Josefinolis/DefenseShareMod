package defenseshare.patches;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import defenseshare.DefenseShareMod;
import defenseshare.util.AllyManager;
import defenseshare.util.DefenseCardDetector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches de renderizado para mostrar indicadores visuales
 * cuando las cartas de defensa pueden ser compartidas con aliados
 */
public class RenderPatch {

    private static final Logger logger = LogManager.getLogger(RenderPatch.class.getName());

    // Colores para indicadores
    private static final Color SHAREABLE_COLOR = new Color(0.3f, 0.7f, 1.0f, 0.8f);
    private static final Color SELECTING_COLOR = new Color(0.2f, 1.0f, 0.4f, 0.9f);

    /**
     * Patch para renderizar indicadores en cartas de defensa compartibles
     */
    @SpirePatch(
        clz = AbstractCard.class,
        method = "render",
        paramtypez = {SpriteBatch.class}
    )
    public static class CardRenderPatch {

        @SpirePostfixPatch
        public static void Postfix(AbstractCard __instance, SpriteBatch sb) {
            // Solo renderizar si TiS está activo y estamos en combate
            if (!DefenseShareMod.isTogetherInSpireLoaded() ||
                AbstractDungeon.getCurrRoom() == null ||
                AbstractDungeon.getCurrRoom().phase != AbstractRoom.RoomPhase.COMBAT) {
                return;
            }

            // Solo mostrar si es carta de defensa, hay aliados, y Shift está presionado
            if (DefenseCardDetector.isDefenseCard(__instance) &&
                AllyManager.hasAlliesAvailable() &&
                CardTargetingPatch.isAllyModeActive()) {

                // Mostrar en cartas de la mano
                if (AbstractDungeon.player.hand.contains(__instance)) {
                    renderAllyIndicator(sb, __instance);
                }
            }
        }

        private static void renderAllyIndicator(SpriteBatch sb, AbstractCard card) {
            float x = card.current_x;
            float y = card.current_y + (card.hb.height / 2) + (30 * Settings.scale);

            FontHelper.renderFontCentered(
                sb,
                FontHelper.cardTitleFont,
                "ALLY",
                x,
                y,
                SHAREABLE_COLOR
            );
        }
    }

    /**
     * Patch para renderizar overlay sobre aliados cuando se tiene una carta de defensa seleccionada
     */
    @SpirePatch(
        clz = AbstractRoom.class,
        method = "render",
        paramtypez = {SpriteBatch.class}
    )
    public static class RoomRenderPatch {

        @SpirePostfixPatch
        public static void Postfix(AbstractRoom __instance, SpriteBatch sb) {
            // Solo renderizar si Together in Spire está activo
            if (!DefenseShareMod.isTogetherInSpireLoaded() || !AllyManager.hasAlliesAvailable()) {
                return;
            }

            // Verificar si el jugador tiene una carta de defensa en la mano
            if (AbstractDungeon.player != null && AbstractDungeon.player.hand != null) {
                for (AbstractCard card : AbstractDungeon.player.hand.group) {
                    if (DefenseCardDetector.isDefenseCard(card) && card.isGlowing) {
                        AllyManager.render(sb);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Patch para mostrar tooltips de compartir defensa
     */
    @SpirePatch(
        clz = AbstractCard.class,
        method = "renderCardTip",
        paramtypez = {SpriteBatch.class}
    )
    public static class CardTipRenderPatch {

        @SpirePostfixPatch
        public static void Postfix(AbstractCard __instance, SpriteBatch sb) {
            if (!DefenseShareMod.isTogetherInSpireLoaded() ||
                !DefenseCardDetector.isDefenseCard(__instance) ||
                !AllyManager.hasAlliesAvailable()) {
                return;
            }

            // Agregar tooltip adicional indicando que se puede compartir
            // El tooltip aparece cuando el mouse está sobre la carta
            if (__instance.hb.hovered) {
                // El tooltip adicional se manejaría aquí
                // Por simplicidad, el indicador [ALLY] en la carta es suficiente
            }
        }
    }
}
