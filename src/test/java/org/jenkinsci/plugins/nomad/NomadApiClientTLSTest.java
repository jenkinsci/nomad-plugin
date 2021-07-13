package org.jenkinsci.plugins.nomad;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.tomakehurst.wiremock.WireMockServer;

import okhttp3.Request;

/**
 * Checks that the TLS support is working as expected.
 */
@RunWith(MockitoJUnitRunner.class)
public class NomadApiClientTLSTest {

    /**
     * Keystore: contains the public and private keys (e.g. KEYSTORE_CLIENT_A contains key pair of client a)
     */
    private static final String KEYSTORE_CLIENT_A = loadResource("/tls/client_a.p12");
    private static final String KEYSTORE_SERVER_A = loadResource("/tls/server_a.p12");

    /**
     * Truststore: contains the public keys (e.g. TRUSTSTORE_CLIENT_A trusts client a but not client b)
     */
    private static final String TRUSTSTORE_CLIENT_A = loadResource("/tls/truststore_client_a.p12");
    private static final String TRUSTSTORE_SERVER_A = loadResource("/tls/truststore_server_a.p12");
    private static final String TRUSTSTORE_CLIENT_B = loadResource("/tls/truststore_client_b.p12");
    private static final String TRUSTSTORE_SERVER_B = loadResource("/tls/truststore_server_b.p12");

    /**
     * General password for all certificates.
     */
    private static final String PASSWORD = "changeit";

    @Mock
    private NomadCloud nomadCloud;

    @InjectMocks
    private NomadApi nomadApi;

    private WireMockServer wireMockServer;

    @After
    public void stopWireMock() {
        wireMockServer.stop();
    }

    /**
     * Checks that mutual TLS is working as expected (client proves the identity of the server and vise versa).
     */
    @Test
    public void testTwoWayHandshake() {
        // GIVEN
        startWiremock(KEYSTORE_SERVER_A, TRUSTSTORE_CLIENT_A, true);
        when(nomadCloud.isTlsEnabled()).thenReturn(true);
        when(nomadCloud.getClientCertificate()).thenReturn(KEYSTORE_CLIENT_A);
        when(nomadCloud.getClientPassword()).thenReturn(PASSWORD);
        when(nomadCloud.getServerCertificate()).thenReturn(TRUSTSTORE_SERVER_A);
        when(nomadCloud.getServerPassword()).thenReturn(PASSWORD);

        // WHEN
        Request request = new Request.Builder().url(wireMockServer.baseUrl()).build();
        String response = nomadApi.checkResponseAndGetBody(request);

        // THEN
        assertThat(response, is("Hello Nomad!"));
    }

    /**
     * Checks that one way TLS is working as expected (server proves the identity of the client but not vise versa).
     */
    @Test
    public void testOneWayHandshakeClient() {
        // GIVEN
        startWiremock(KEYSTORE_SERVER_A, TRUSTSTORE_CLIENT_A, true);
        when(nomadCloud.isTlsEnabled()).thenReturn(true);
        when(nomadCloud.getClientCertificate()).thenReturn(KEYSTORE_CLIENT_A);
        when(nomadCloud.getClientPassword()).thenReturn(PASSWORD);

        // WHEN
        Request request = new Request.Builder().url(wireMockServer.baseUrl()).build();
        String response = nomadApi.checkResponseAndGetBody(request);

        // THEN
        assertThat(response, is("Hello Nomad!"));
    }

    /**
     * Checks that one way TLS is working as expected (client proves the identity of the server but not vise versa).
     */
    @Test
    public void testOneWayHandshakeServer() {
        // GIVEN
        startWiremock(KEYSTORE_SERVER_A, TRUSTSTORE_CLIENT_A, false);
        when(nomadCloud.isTlsEnabled()).thenReturn(true);
        when(nomadCloud.getServerCertificate()).thenReturn(TRUSTSTORE_SERVER_A);
        when(nomadCloud.getServerPassword()).thenReturn(PASSWORD);

        // WHEN
        Request request = new Request.Builder().url(wireMockServer.baseUrl()).build();
        String response = nomadApi.checkResponseAndGetBody(request);

        // THEN
        assertThat(response, is("Hello Nomad!"));
    }

    /**
     * Checks that TLS in general is working as expected (client not proves the identity of the server and vise versa).
     */
    @Test
    public void testGeneral() {
        // GIVEN
        startWiremock(KEYSTORE_SERVER_A, TRUSTSTORE_CLIENT_A, false);
        when(nomadCloud.isTlsEnabled()).thenReturn(true);

        // WHEN
        Request request = new Request.Builder().url(wireMockServer.baseUrl()).build();
        String response = nomadApi.checkResponseAndGetBody(request);

        // THEN
        assertThat(response, is("Hello Nomad!"));
    }

    /**
     * Checks that the response of an untrusted client is working as expected  (client is not trustworthy).
     */
    @Test
    public void testClientUntrusted() {
        // GIVEN
        startWiremock(KEYSTORE_SERVER_A, TRUSTSTORE_CLIENT_B, true);
        when(nomadCloud.isTlsEnabled()).thenReturn(true);
        when(nomadCloud.getClientCertificate()).thenReturn(KEYSTORE_CLIENT_A);
        when(nomadCloud.getClientPassword()).thenReturn(PASSWORD);

        // WHEN
        Request request = new Request.Builder().url(wireMockServer.baseUrl()).build();
        String response = nomadApi.checkResponseAndGetBody(request);

        // THEN
        assertThat(response, is(""));
    }

    /**
     * Checks that the response of an untrusted server is working as expected  (server is not trustworthy).
     */
    @Test
    public void testServerUntrusted() {
        // GIVEN
        startWiremock(KEYSTORE_SERVER_A, TRUSTSTORE_CLIENT_A, true);
        when(nomadCloud.isTlsEnabled()).thenReturn(true);
        when(nomadCloud.getClientCertificate()).thenReturn(KEYSTORE_CLIENT_A);
        when(nomadCloud.getClientPassword()).thenReturn(PASSWORD);
        when(nomadCloud.getServerCertificate()).thenReturn(TRUSTSTORE_SERVER_B);
        when(nomadCloud.getServerPassword()).thenReturn(PASSWORD);

        // WHEN
        Request request = new Request.Builder().url(wireMockServer.baseUrl()).build();
        String response = nomadApi.checkResponseAndGetBody(request);

        // THEN
        assertThat(response, is(""));
    }

    private void startWiremock(String keystore, String truststore, boolean clientAuth) {
        wireMockServer = new WireMockServer(
                wireMockConfig()
                        .httpsPort(0)
                        .keystorePath(keystore)
                        .keystoreType("PKCS12")
                        .keystorePassword(PASSWORD)
                        .keyManagerPassword(PASSWORD)
                        .needClientAuth(clientAuth)
                        .trustStorePath(truststore)
                        .trustStoreType("PKCS12")
                        .trustStorePassword(PASSWORD)
                );
        wireMockServer.start();

        stubFor(get(anyUrl()).willReturn(ok("Hello Nomad!")));
    }

    private static String loadResource(String resource) {
        try {
            return Paths.get(NomadApiClientTLSTest.class.getResource(resource).toURI()).toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }


}