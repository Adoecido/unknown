package vegas.sony.remote;

import java.io.IOException;

/** Safe, user-facing errors; never forward raw cloud responses or session tokens. */
final class ControlRequestException extends IOException {
    final int status;
    final String code;

    ControlRequestException(int status, String code) {
        super(messageFor(code));
        this.status = status;
        this.code = code == null ? "request_failed" : code;
    }

    /** Only expose the small, documented error vocabulary to the local UI. */
    String safeCode() {
        switch (this.code) {
            case "unauthorized":
            case "session_unavailable":
            case "config_not_found":
            case "invalid_slot":
            case "invalid_config":
            case "config_too_large":
            case "rate_limit":
                return this.code;
            default:
                return "request_failed";
        }
    }

    private static String messageFor(String code) {
        if (code == null) code = "request_failed";
        switch (code) {
            case "unauthorized":
            case "session_unavailable":
                return "Sessao remota indisponivel ou expirada. Autentique novamente pelo loader.";
            case "config_not_found": return "Este slot nao tem uma config salva. Atualize a pagina.";
            case "invalid_slot":
            case "invalid_config": return "Config invalida. Confira o nome e o slot selecionado.";
            case "config_too_large": return "A config excede o limite de 512 KB.";
            case "already_in_party": return "Voce ja esta em uma party. Atualize a pagina ou saia dela primeiro.";
            case "invalid_party_code": return "O codigo da party deve ter 6 letras ou numeros.";
            case "party_closed":
            case "party_not_found": return "Party nao encontrada ou encerrada. Confira o codigo.";
            case "party_full": return "Esta party esta cheia.";
            case "not_in_party": return "Voce nao esta nesta party. Atualize a pagina.";
            case "leader_required": return "Somente o lider pode sincronizar os modulos.";
            case "no_modules_selected": return "Selecione pelo menos um modulo para compartilhar.";
            case "command_too_large": return "A sincronizacao excede o limite da party.";
            case "rate_limit": return "Muitas solicitacoes. Aguarde alguns segundos e tente novamente.";
            default: return "A nuvem nao concluiu a solicitacao. Seus campos foram mantidos; tente novamente.";
        }
    }
}
