package defenseshare.util;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utilidad para detectar si una carta otorga defensa (Block)
 */
public class DefenseCardDetector {

    private static final Logger logger = LogManager.getLogger(DefenseCardDetector.class.getName());

    // IDs de cartas conocidas que otorgan defensa
    private static final Set<String> KNOWN_DEFENSE_CARDS = new HashSet<>();

    // IDs de cartas que NO deben ser compartibles (efectos especiales)
    private static final Set<String> EXCLUDED_CARDS = new HashSet<>();

    public static void initialize() {
        // Cartas base de defensa del juego
        initializeKnownDefenseCards();
        initializeExcludedCards();
        logger.info("DefenseCardDetector inicializado");
    }

    private static void initializeKnownDefenseCards() {
        // === IRONCLAD ===
        KNOWN_DEFENSE_CARDS.add("Defend_R");      // Defend
        KNOWN_DEFENSE_CARDS.add("Shrug It Off");
        KNOWN_DEFENSE_CARDS.add("Iron Wave");
        KNOWN_DEFENSE_CARDS.add("Impervious");
        KNOWN_DEFENSE_CARDS.add("Flame Barrier");
        KNOWN_DEFENSE_CARDS.add("Entrench");
        KNOWN_DEFENSE_CARDS.add("Ghostly Armor");
        KNOWN_DEFENSE_CARDS.add("Metallicize");
        KNOWN_DEFENSE_CARDS.add("Power Through");
        KNOWN_DEFENSE_CARDS.add("Second Wind");
        KNOWN_DEFENSE_CARDS.add("True Grit");

        // === SILENT ===
        KNOWN_DEFENSE_CARDS.add("Defend_G");      // Defend
        KNOWN_DEFENSE_CARDS.add("Dodge and Roll");
        KNOWN_DEFENSE_CARDS.add("Blur");
        KNOWN_DEFENSE_CARDS.add("Backflip");
        KNOWN_DEFENSE_CARDS.add("Cloak and Dagger");
        KNOWN_DEFENSE_CARDS.add("Deflect");
        KNOWN_DEFENSE_CARDS.add("Leg Sweep");
        KNOWN_DEFENSE_CARDS.add("After Image");
        KNOWN_DEFENSE_CARDS.add("Footwork");
        KNOWN_DEFENSE_CARDS.add("Piercing Wail");
        KNOWN_DEFENSE_CARDS.add("Escape Plan");
        KNOWN_DEFENSE_CARDS.add("Calculated Gamble");

        // === DEFECT ===
        KNOWN_DEFENSE_CARDS.add("Defend_B");      // Defend
        KNOWN_DEFENSE_CARDS.add("Glacier");
        KNOWN_DEFENSE_CARDS.add("Leap");
        KNOWN_DEFENSE_CARDS.add("Chill");
        KNOWN_DEFENSE_CARDS.add("Coolheaded");
        KNOWN_DEFENSE_CARDS.add("Hologram");
        KNOWN_DEFENSE_CARDS.add("Auto-Shields");
        KNOWN_DEFENSE_CARDS.add("Reinforced Body");
        KNOWN_DEFENSE_CARDS.add("Equilibrium");
        KNOWN_DEFENSE_CARDS.add("Consume");
        KNOWN_DEFENSE_CARDS.add("Core Surge");

        // === WATCHER ===
        KNOWN_DEFENSE_CARDS.add("Defend_P");      // Defend
        KNOWN_DEFENSE_CARDS.add("Protect");
        KNOWN_DEFENSE_CARDS.add("Third Eye");
        KNOWN_DEFENSE_CARDS.add("Empty Body");
        KNOWN_DEFENSE_CARDS.add("Halt");
        KNOWN_DEFENSE_CARDS.add("Wallop");
        KNOWN_DEFENSE_CARDS.add("Indignation");
        KNOWN_DEFENSE_CARDS.add("Like Water");
        KNOWN_DEFENSE_CARDS.add("Mental Fortress");
        KNOWN_DEFENSE_CARDS.add("Perseverance");
        KNOWN_DEFENSE_CARDS.add("Sanctity");
        KNOWN_DEFENSE_CARDS.add("Talk to the Hand");
        KNOWN_DEFENSE_CARDS.add("Wave of the Hand");
        KNOWN_DEFENSE_CARDS.add("Spirit Shield");

        // === COLORLESS ===
        KNOWN_DEFENSE_CARDS.add("Defend_C");
        KNOWN_DEFENSE_CARDS.add("Panacea");
    }

    private static void initializeExcludedCards() {
        // Cartas que no deberían poder compartirse por efectos secundarios
        EXCLUDED_CARDS.add("Barricade");     // Efecto permanente sobre uno mismo
        EXCLUDED_CARDS.add("Entrench");      // Duplica bloqueo existente
        EXCLUDED_CARDS.add("Body Slam");     // Hace daño basado en bloqueo propio
    }

    /**
     * Determina si una carta otorga defensa y puede ser compartida
     */
    public static boolean isDefenseCard(AbstractCard card) {
        if (card == null) {
            return false;
        }

        // Verificar si está en la lista de exclusión
        if (EXCLUDED_CARDS.contains(card.cardID)) {
            return false;
        }

        // Verificar si está en la lista conocida de cartas de defensa
        if (KNOWN_DEFENSE_CARDS.contains(card.cardID)) {
            return true;
        }

        // Detección dinámica: verificar si la carta tiene baseBlock > 0
        if (card.baseBlock > 0) {
            return true;
        }

        // Verificar si el tipo de carta es SKILL y tiene la palabra "Block" o "Defend"
        if (card.type == AbstractCard.CardType.SKILL) {
            String description = card.rawDescription.toLowerCase();
            if (description.contains("block") || description.contains("defend")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calcula la cantidad de bloqueo que otorgaría la carta
     */
    public static int calculateBlockAmount(AbstractCard card) {
        if (card == null) {
            return 0;
        }

        // Usar el valor de block de la carta (ya calculado con mejoras)
        if (card.block > 0) {
            return card.block;
        }

        // Fallback al baseBlock si block no está calculado
        if (card.baseBlock > 0) {
            return card.baseBlock;
        }

        return 0;
    }

    /**
     * Verifica si una carta puede ser potencialmente compartida
     * Esto es útil para mostrar indicadores visuales antes de jugar la carta
     */
    public static boolean canBeShared(AbstractCard card) {
        if (!isDefenseCard(card)) {
            return false;
        }

        // Verificar que hay aliados disponibles
        return AllyManager.hasAlliesAvailable();
    }

    /**
     * Registra una carta personalizada como carta de defensa
     * Útil para mods que añaden nuevas cartas
     */
    public static void registerDefenseCard(String cardID) {
        KNOWN_DEFENSE_CARDS.add(cardID);
        logger.info("Carta de defensa registrada: " + cardID);
    }

    /**
     * Excluye una carta de poder ser compartida
     */
    public static void excludeCard(String cardID) {
        EXCLUDED_CARDS.add(cardID);
        logger.info("Carta excluida de compartir: " + cardID);
    }
}
