package ar.com.anepanet.planificator.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TelegramClientTest {

    @Test
    void stripsConfiguredTokenAndBotTokenPatternFromErrorText() {
        String token = "123456789:AAExampleToken_for-tests";
        String leaked = "403 on https://api.telegram.org/bot" + token + "/sendMessage";

        String safe = TelegramClient.sanitize(leaked, token);

        assertFalse(safe.contains(token));
        assertEquals("403 on https://api.telegram.org/bot***/sendMessage", safe);
    }

    @Test
    void replacesBlankMessagesWithGenericText() {
        assertEquals("error de Telegram", TelegramClient.sanitize(null, "x"));
        assertEquals("error de Telegram", TelegramClient.sanitize("  ", "x"));
    }
}
