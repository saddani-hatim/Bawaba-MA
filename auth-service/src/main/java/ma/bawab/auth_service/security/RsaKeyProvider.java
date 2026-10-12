package ma.bawab.auth_service.security;

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

    @Value("${jwt.private-key-path:/home/hatim/projects/Bawaba/key/private.key}")
    private String privateKeyPath;

    @Value("${jwt.public-key-path:/home/hatim/projects/Bawaba/key/public.key}")
    private String publicKeyPath;

    private final ResourceLoader resourceLoader;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    public RsaKeyProvider(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        try {
            this.privateKey = loadPrivateKey(privateKeyPath);
            this.publicKey = loadPublicKey(publicKeyPath);
            log.info("Successfully loaded RSA private and public keys for RS256 signatures.");
        } catch (Exception e) {
            log.error("Failed to load RSA keys from configured paths: private={}, public={}", privateKeyPath,
                    publicKeyPath, e);
            throw new IllegalStateException("Could not initialize RSA keys for JWT authentication", e);
        }
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
        Path path = Paths.get(pathStr);
        if (Files.exists(path)) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }

        // Fallback: try loading from Spring ResourceLoader (e.g. classpath)
        Resource resource = resourceLoader.getResource(pathStr);
        if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        throw new IllegalArgumentException("Key file not found at path: " + pathStr);
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
