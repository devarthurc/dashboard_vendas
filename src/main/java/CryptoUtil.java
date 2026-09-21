import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
final class CryptoUtil {


    private static final byte[] PARTE_A = {
            (byte) 0x4a, (byte) 0x2f, (byte) 0x91, (byte) 0x6c,
            (byte) 0x08, (byte) 0xd3, (byte) 0x77, (byte) 0x1e,
            (byte) 0x55, (byte) 0xa0, (byte) 0x3c, (byte) 0x6f,
            (byte) 0x99, (byte) 0x12, (byte) 0x84, (byte) 0x2b
    };
    private static final byte[] PARTE_B = {
            (byte) 0x11, (byte) 0x5e, (byte) 0x87, (byte) 0x33,
            (byte) 0x40, (byte) 0x0c, (byte) 0xf9, (byte) 0x62,
            (byte) 0x1d, (byte) 0x77, (byte) 0xa2, (byte) 0x50,
            (byte) 0x3e, (byte) 0x88, (byte) 0x14, (byte) 0x99
    };

    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    private CryptoUtil() {}

    private static byte[] chave() {
        byte[] chave = new byte[32];
        System.arraycopy(PARTE_A, 0, chave, 0, 16);
        System.arraycopy(PARTE_B, 0, chave, 16, 16);
        return chave;
    }


    static String encrypt(String textoClaro) {
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(chave(), "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] cifrado = cipher.doFinal(textoClaro.getBytes(StandardCharsets.UTF_8));

            byte[] saida = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, saida, 0, iv.length);
            System.arraycopy(cifrado, 0, saida, iv.length, cifrado.length);

            return Base64.getEncoder().encodeToString(saida);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao cifrar configuração", e);
        }
    }


    static String decrypt(String textoCifradoBase64) {
        try {
            byte[] dados = Base64.getDecoder().decode(textoCifradoBase64.trim());

            byte[] iv = new byte[GCM_IV_BYTES];
            System.arraycopy(dados, 0, iv, 0, GCM_IV_BYTES);

            byte[] cifrado = new byte[dados.length - GCM_IV_BYTES];
            System.arraycopy(dados, GCM_IV_BYTES, cifrado, 0, cifrado.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(chave(), "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] textoClaro = cipher.doFinal(cifrado);
            return new String(textoClaro, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Falha ao decifrar configuração — arquivo corrompido, incompleto ou não cifrado por esta ferramenta",
                    e);
        }
    }
}
