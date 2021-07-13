package org.jenkinsci.plugins.nomad;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import hudson.cli.NoCheckTrustManager;
import okhttp3.OkHttpClient;

/**
 * Class which helps with the {@link OkHttpClient} and with the configuration of the transport layer security in particular.
 */
public class OkHttpClientHelper {

    private OkHttpClientHelper() {
    }

    /**
     * Initializes the transport layer security for a given OkHttpClient.Builder.
     *
     * @param builder OkHttpClient builder (not null, sslSocketFactory gets initialized)
     * @param clientCertPath Path to the PKCS12 client certificate which contains public and private key
     * @param clientCertPass Password for PKCS12 client certificate (optional)
     * @param serverCertPath Path to the PKCS12 server certificate (usually a common authority or a public key).
     * @param serverCertPass Password for PKCS12 client certificate (optional)
     * @throws GeneralSecurityException if the client or server certificate is not valid
     * @throws IOException              if the client or server certificate is not readable
     */
    public static void initTLS(OkHttpClient.Builder builder, String clientCertPath, String clientCertPass, String serverCertPath,
            String serverCertPass) throws GeneralSecurityException, IOException {

        KeyManager[] keyManagers = createKeyManagers(clientCertPath, clientCertPass);
        TrustManager[] trustManagers = createTrustManagers(serverCertPath, serverCertPass);
        SSLContext ctx = createSSLContext(keyManagers, trustManagers);
        builder.sslSocketFactory(ctx.getSocketFactory(), (X509TrustManager) trustManagers[0]);
    }

    /**
     * Creates an instance of {@link SSLContext} from a given PKCS12 client and server certificate.
     *
     * @return SSLContext
     * @throws GeneralSecurityException if the ssl context cannot be created
     */
    public static SSLContext createSSLContext(KeyManager[] km, TrustManager[] tm) throws GeneralSecurityException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(km, tm, null);

        return context;
    }

    /**
     * Creates a list of key managers from a given client certificate.
     *
     * @param path Path to the PKCS12 client certificate which contains public and private key (optional)
     * @param pass Password for PKCS12 client certificate (optional)
     * @return list of key managers or null if path is empty
     * @throws GeneralSecurityException if the client certificate is not valid
     * @throws IOException              if the client certificate is not readable
     */
    public static KeyManager[] createKeyManagers(String path, String pass) throws GeneralSecurityException, IOException {
        if (path == null || path.isEmpty()) {
            return null;
        }

        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(createKeystore(path, pass), toCharArray(pass));

        return factory.getKeyManagers();
    }

    /**
     * Creates a list of trust managers from a given server certificate. Note: If the path is empty then TLS is enabled but the server
     * certificate is not validated.
     *
     * @param path Path to the PKCS12 server certificate (usually a common authority or a public key or empty).
     * @param pass Password for PKCS12 client certificate (optional)
     * @return list of trust managers (not null and not empty)
     * @throws GeneralSecurityException if the server certificate is not valid
     * @throws IOException              if the server certificate is not readable
     */
    public static TrustManager[] createTrustManagers(String path, String pass) throws GeneralSecurityException, IOException {
        if (path == null || path.isEmpty()) {
            return new TrustManager[]{new NoCheckTrustManager()};
        }

        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(createKeystore(path, pass));

        return factory.getTrustManagers();
    }

    private static KeyStore createKeystore(String path, String password) throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance("PKCS12");

        try (FileInputStream fis = new FileInputStream(path)) {
            ks.load(fis, toCharArray(password));
        }

        return ks;
    }

    private static char[] toCharArray(String password) {
        return password != null ? password.toCharArray() : new char[]{};
    }

}
