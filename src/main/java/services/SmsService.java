package services;

import com.messagebird.MessageBirdClient;
import com.messagebird.MessageBirdService;
import com.messagebird.MessageBirdServiceImpl;
import com.messagebird.objects.MessageResponse;
import com.messagebird.exceptions.UnauthorizedException;
import com.messagebird.exceptions.GeneralException;

import java.math.BigInteger;
import java.util.Arrays;

public class SmsService {

    private static final String API_KEY   = "YOUR_API_KEY"; // ← ta clé MessageBird
    private static final String FROM_NAME = "LocalTrade";   // max 11 caractères

    public static void sendResetCode(String toPhone, String code) throws Exception {

        String body = "[LocalTrade] Code : " + code + ". Valable 10 minutes.";

        MessageBirdService service = new MessageBirdServiceImpl(API_KEY);
        MessageBirdClient  client  = new MessageBirdClient(service);

        // Supprimer tout sauf les chiffres
        String cleanPhone = toPhone.replaceAll("[^0-9]", "");

        try {
            MessageResponse response = client.sendMessage(
                    FROM_NAME,
                    body,
                    Arrays.asList(new BigInteger(cleanPhone))
            );
            System.out.println("✅ SMS envoyé — ID : " + response.getId());

        } catch (UnauthorizedException e) {
            throw new Exception("❌ Clé API MessageBird invalide : " + e.getMessage());
        } catch (GeneralException e) {
            throw new Exception("❌ Erreur MessageBird : " + e.getMessage());
        }
    }
}