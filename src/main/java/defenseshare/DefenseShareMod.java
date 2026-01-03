package defenseshare;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.OnCardUseSubscriber;
import basemod.interfaces.OnStartBattleSubscriber;
import basemod.interfaces.PostBattleSubscriber;
import basemod.interfaces.StartGameSubscriber;
import basemod.interfaces.PostDrawSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import defenseshare.util.AllyManager;
import defenseshare.util.DefenseCardDetector;

/**
 * Defense Share Mod - Together in Spire Addon
 *
 * Permite que las cartas de defensa (Block) puedan ser lanzadas
 * sobre aliados en partidas cooperativas de Together in Spire.
 *
 * OPTIMIZADO: Sin reflection en runtime, sin logging excesivo
 */
@SpireInitializer
public class DefenseShareMod implements
        PostInitializeSubscriber,
        OnCardUseSubscriber,
        OnStartBattleSubscriber,
        PostBattleSubscriber,
        StartGameSubscriber,
        PostDrawSubscriber {

    public static final Logger logger = LogManager.getLogger(DefenseShareMod.class.getName());
    public static final String MOD_ID = "DefenseShareMod";
    private static final String VERSION = "1.5.8";

    // Estado del mod
    private static boolean togetherInSpireLoaded = false;

    public DefenseShareMod() {
        logger.info("Defense Share Mod v" + VERSION + " cargando...");
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new DefenseShareMod();
    }

    @Override
    public void receivePostInitialize() {
        logger.info("Defense Share Mod - Post Initialize");

        // Detectar si Together in Spire está cargado (una sola vez)
        togetherInSpireLoaded = detectTogetherInSpire();

        if (togetherInSpireLoaded) {
            logger.info("Together in Spire detectado - mod activo");
        } else {
            logger.info("Together in Spire no detectado - mod en espera");
        }

        // Inicializar componentes
        DefenseCardDetector.initialize();
        AllyManager.initialize();
    }

    /**
     * Detecta si Together in Spire está cargado
     */
    private boolean detectTogetherInSpire() {
        String[] classNames = {
            "spireTogether.SpireTogetherMod",
            "togetherinspire.TogetherInSpire",
            "tis.TogetherInSpire"
        };

        for (String className : classNames) {
            try {
                Class.forName(className);
                return true;
            } catch (ClassNotFoundException ignored) {}
        }
        return false;
    }

    // === Eventos para invalidar cache ===

    @Override
    public void receiveOnBattleStart(AbstractRoom room) {
        // Invalidar cache al inicio de cada combate
        AllyManager.invalidateCache();
    }

    @Override
    public void receivePostBattle(AbstractRoom room) {
        // Invalidar cache al terminar combate
        AllyManager.invalidateCache();
    }

    @Override
    public void receiveStartGame() {
        // Invalidar cache al iniciar partida
        AllyManager.invalidateCache();
    }

    @Override
    public void receivePostDraw(AbstractCard card) {
        // Invalidar cache cuando se roba una carta (por si cambió el estado de aliados)
        AllyManager.invalidateCache();
    }

    @Override
    public void receiveCardUsed(AbstractCard card) {
        // Limpiar estado después de usar carta de defensa
        if (DefenseCardDetector.isDefenseCard(card)) {
            defenseshare.patches.GainBlockPatch.clearTargetAlly();
        }
    }

    // === Getters estáticos ===

    public static boolean isTogetherInSpireLoaded() {
        return togetherInSpireLoaded;
    }
}
