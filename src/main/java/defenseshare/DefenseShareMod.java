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
    private static boolean isSelectingAlly = false;
    private static AbstractCard pendingDefenseCard = null;
    private static int pendingBlockAmount = 0;

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
        // Solo procesar si Together in Spire está activo y estamos en modo coop
        if (!togetherInSpireLoaded || !AllyManager.isInCoopGame()) {
            return;
        }

        // Verificar si la carta otorga defensa
        if (DefenseCardDetector.isDefenseCard(card)) {
            logger.info("Carta de defensa detectada: " + card.name);

            // Si hay aliados disponibles, ofrecer la opción de compartir defensa
            if (AllyManager.hasAlliesAvailable()) {
                // Guardar información de la carta para procesamiento
                handleDefenseCardPlayed(card);
            }
        }
    }

    @Override
    public void receivePostUpdate() {
        // Actualizar la selección de aliados si está activa
        if (isSelectingAlly && pendingDefenseCard != null) {
            AllyManager.updateAllySelection();

            AbstractPlayer selectedAlly = AllyManager.getSelectedAlly();
            if (selectedAlly != null) {
                applyDefenseToAlly(selectedAlly, pendingBlockAmount);
                resetPendingState();
            }
        }
    }

    /**
     * Maneja cuando se juega una carta de defensa
     */
    private void handleDefenseCardPlayed(AbstractCard card) {
        // Calcular la cantidad de bloqueo
        int blockAmount = DefenseCardDetector.calculateBlockAmount(card);

        if (blockAmount > 0) {
            pendingDefenseCard = card;
            pendingBlockAmount = blockAmount;
            isSelectingAlly = true;

            // Iniciar selección de aliado
            AllyManager.startAllySelection();
            logger.info("Iniciando selección de aliado para " + blockAmount + " de defensa");
        }
    }

    /**
     * Aplica la defensa al aliado seleccionado
     */
    private void applyDefenseToAlly(AbstractPlayer ally, int blockAmount) {
        if (ally != null && blockAmount > 0) {
            logger.info("Aplicando " + blockAmount + " de defensa a aliado: " + ally.name);

            // Crear y añadir la acción de ganar bloqueo
            AbstractDungeon.actionManager.addToBottom(
                new GainBlockAction(ally, AbstractDungeon.player, blockAmount)
            );
        }
    }

    /**
     * Resetea el estado pendiente
     */
    private void resetPendingState() {
        isSelectingAlly = false;
        pendingDefenseCard = null;
        pendingBlockAmount = 0;
        AllyManager.endAllySelection();
    }

    // === Getters estáticos para otros componentes ===

    public static boolean isTogetherInSpireLoaded() {
        return togetherInSpireLoaded;
    }

    public static boolean isSelectingAlly() {
        return isSelectingAlly;
    }

    public static void cancelAllySelection() {
        isSelectingAlly = false;
        pendingDefenseCard = null;
        pendingBlockAmount = 0;
        AllyManager.endAllySelection();
    }
}
