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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class RsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);

    @Value("${jwt.public-key-path:/home/hatim/projects/Bawaba/key/public.key}")
    private String publicKeyPath;

    @Value("${jwt.private-key-path:/home/hatim/projects/Bawaba/key/private.key}")
    private String privateKeyPath;

    private final ResourceLoader resourceLoader;
    private PublicKey publicKey;
    private PrivateKey privateKey;

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

        try {
            this.privateKey = loadPrivateKey(privateKeyPath);
            log.info("Successfully loaded RSA private key for access token rotation.");
        } catch (Exception e) {
            log.error("Failed to load RSA private key from path: {}", privateKeyPath, e);
            throw new IllegalStateException("Could not initialize RSA private key for token rotation", e);
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

    private PrivateKey loadPrivateKey(String path) throws Exception {
        String keyContent = readKeyContent(path);
        String cleaned = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(cleaned);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
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

        // Fallback: try ResourceLoader
        String resourceLocation = pathStr.startsWith("classpath:") ? pathStr
                : "classpath:" + Paths.get(pathStr).getFileName();
        Resource resource = resourceLoader.getResource(resourceLocation);
        if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        throw new IllegalArgumentException("RSA key file not found at path: " + pathStr);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}
