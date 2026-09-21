import java.util.ArrayList;
import java.util.List;

class TransactionManager {

    private boolean active = false;

    private final List<String> commands =
            new ArrayList<>();

    // =========================
    // MULTI
    // =========================

    String multi() {

        if (active) {

            return "ERROR: Transaction already active";
        }

        active = true;
        commands.clear();

        return "OK";
    }

    // =========================
    // ADD COMMAND
    // =========================

    String queueCommand(String command) {

        if (!active) {

            return "ERROR: MULTI required";
        }

        commands.add(command);

        return "QUEUED";
    }

    // =========================
    // EXEC
    // =========================

    List<String> exec() {

        if (!active) {

            return null;
        }

        active = false;

        List<String> queuedCommands =
                new ArrayList<>(commands);

        commands.clear();

        return queuedCommands;
    }

    // =========================
    // DISCARD
    // =========================

    String discard() {

        if (!active) {

            return "ERROR: No active transaction";
        }

        active = false;

        commands.clear();

        return "OK";
    }

    // =========================
    // STATUS
    // =========================

    boolean isActive() {

        return active;
    }
}