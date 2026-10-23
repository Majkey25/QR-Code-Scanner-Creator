import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.CodeSigner;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class VerifySigningJar {
    private static final String EXPECTED_CERTIFICATE =
        "3d83947a2d2b3ae2016a1b7026cd938151a9beaeeb82e8a1c088e7bcf9d8459b";
    private static final byte[] EXPECTED_CONTENT =
        "QR signing credential continuity check. No application code.\n".getBytes(StandardCharsets.UTF_8);

    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                throw new IllegalArgumentException();
            }
            try (JarFile jar = new JarFile(Path.of(args[0]).toFile(), true)) {
                JarEntry entry = jar.getJarEntry("migration-proof.txt");
                if (entry == null) {
                    throw new SecurityException();
                }
                // Reading the entry to EOF makes JarFile validate its signature.
                try (InputStream stream = jar.getInputStream(entry)) {
                    if (!Arrays.equals(EXPECTED_CONTENT, stream.readAllBytes())) {
                        throw new SecurityException();
                    }
                }
                CodeSigner[] signers = entry.getCodeSigners();
                if (signers == null || signers.length != 1) {
                    throw new SecurityException();
                }
                byte[] certificate = signers[0].getSignerCertPath().getCertificates().get(0).getEncoded();
                String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate));
                if (!EXPECTED_CERTIFICATE.equals(fingerprint)) {
                    throw new SecurityException();
                }
            }
        } catch (Exception error) {
            System.err.println("Signed dummy content or certificate did not verify.");
            System.exit(1);
        }
    }
}
