package ma.bawab.api_gateway.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class RsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);

    @Value("${jwt.public-key-path:/home/hatim/projects/Bawaba/key/public.key}")
    private String publicKeyPath;

    private final ResourceLoader resourceLoader;
    private PublicKey publicKey;

    public RsaKeyProvider(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        try {
            this.publicKey = loadPublicKey(publicKeyPath);
            log.info("Successfully loaded RSA public key for RS256 token verification.");
        } catch (Exception e) {
            log.error("Failed to load RSA public key from path: {}", publicKeyPath, e);
            throw new IllegalStateException("Could not initialize RSA public key for JWT authentication", e);
        }
    }

    private PublicKey loadPublicKey(String path) throws Exception {
        String keyContent = readKeyContent(path);
        String cleaned = keyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(cleaned);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private String readKeyContent(String pathStr) throws Exception {
        // First try direct file path
        try {
            Path path = Paths.get(pathStr);
            if (Files.exists(path)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            // Ignore and try fallback
        }

        // Fallback: try ResourceLoader (e.g. classpath:public.key)
        String resourceLocation = pathStr.startsWith("classpath:") ? pathStr : "classpath:public.key";
        Resource resource = resourceLoader.getResource(resourceLocation);
        if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        throw new IllegalArgumentException("RSA Public key file not found at path: " + pathStr + " nor in classpath:public.key");
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
