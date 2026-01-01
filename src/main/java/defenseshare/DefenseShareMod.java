package defenseshare;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.OnCardUseSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import defenseshare.util.AllyManager;
import defenseshare.util.DefenseCardDetector;

/**
 * Defense Share Mod - Together in Spire Addon
 *
 * Permite que las cartas de defensa (Block) puedan ser lanzadas
 * sobre aliados en partidas cooperativas de Together in Spire.
 */
@SpireInitializer
public class DefenseShareMod implements PostInitializeSubscriber, OnCardUseSubscriber, PostUpdateSubscriber {

    public static final Logger logger = LogManager.getLogger(DefenseShareMod.class.getName());
    public static final String MOD_ID = "DefenseShareMod";

    // Estado del mod
    private static boolean togetherInSpireLoaded = false;

    public DefenseShareMod() {
        logger.info("=== Inicializando Defense Share Mod ===");
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        logger.info("Defense Share Mod - initialize()");
        new DefenseShareMod();
    }

    @Override
    public void receivePostInitialize() {
        logger.info("Defense Share Mod - Post Initialize");

        // Detectar si Together in Spire está cargado
        togetherInSpireLoaded = detectTogetherInSpire();

        if (togetherInSpireLoaded) {
            logger.info("Together in Spire detectado! El mod de defensa compartida está activo.");
        } else {
            logger.info("Together in Spire no detectado. El mod funcionará cuando TiS esté instalado.");
        }

        // Inicializar el detector de cartas de defensa
        DefenseCardDetector.initialize();

        // Inicializar el manejador de aliados
        AllyManager.initialize();
    }

    /**
     * Detecta si Together in Spire está cargado
     */
    private boolean detectTogetherInSpire() {
        try {
            // Intentar cargar una clase de Together in Spire
            Class.forName("togetherinspire.TogetherInSpire");
            return true;
        } catch (ClassNotFoundException e) {
            // Intentar con otros posibles nombres de paquete
            try {
                Class.forName("tis.TogetherInSpire");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

    @Override
    public void receiveCardUsed(AbstractCard card) {
        // Limpiar el aliado objetivo después de usar cualquier carta de defensa
        if (DefenseCardDetector.isDefenseCard(card)) {
            defenseshare.patches.GainBlockPatch.clearTargetAlly();
            logger.debug("Carta de defensa usada, limpiando estado");
        }
    }

    @Override
    public void receivePostUpdate() {
        // Este método ya no es necesario con el nuevo sistema de targeting
        // Las cartas de defensa ahora se comportan como cartas de ataque
        // y permiten seleccionar aliados directamente
    }

    // === Getters estáticos para otros componentes ===

    public static boolean isTogetherInSpireLoaded() {
        return togetherInSpireLoaded;
    }
}
