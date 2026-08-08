package br.com.cortex.sign.integration.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsappWebhookController {

    @Value("${app.whatsapp.webhook.verify-token:}")
    private String verifyToken;

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verificarWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        boolean tokenConfigurado = verifyToken != null && !verifyToken.isBlank();
        boolean assinaturaValida = "subscribe".equals(mode)
                && tokenConfigurado
                && verifyToken.equals(token)
                && challenge != null;

        if (!assinaturaValida) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Token de verificação inválido");
        }

        return ResponseEntity.ok(challenge);
    }

    @PostMapping
    public ResponseEntity<Void> receberEvento(@RequestBody(required = false) String payload) {
        int tamanhoPayload = payload == null ? 0 : payload.length();
        log.info("Webhook WhatsApp recebido com {} caracteres", tamanhoPayload);
        return ResponseEntity.ok().build();
    }
}
