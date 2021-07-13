package org.jenkinsci.plugins.nomad;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import static okhttp3.RequestBody.create;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import okhttp3.MediaType;
import okhttp3.Request;

/**
 * Checks that the NomadApi is working as expected especially the client and checkResponseAndGetBody method.
 */
@RunWith(MockitoJUnitRunner.class)
public class NomadApiClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Mock
    NomadCloud nomadCloud;

    @InjectMocks
    NomadApi nomadApi;

    private static Object getFieldValue(NomadApi nomadApi, String name) {
        try {
            Field field = NomadApi.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(nomadApi);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object invoke(NomadApi nomadApi, String name) {
        {
            try {
                Method method = NomadApi.class.getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(nomadApi);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Checks that only one client instance exists at a time even when multiple threads requesting the client in parallel.
     */
    @Test
    public void testClientIsThreadSafe() {

        // WHEN
        int count = IntStream.range(0, 100)
                .parallel()
                .mapToObj(i -> invoke(nomadApi, "client"))
                .collect(Collectors.toSet()).size();

        // THEN
        assertThat(count, is(1));
        assertThat(getFieldValue(nomadApi, "client"), notNullValue());
    }

    @Test
    public void testGET() {
        // GIVEN
        stubFor(get(urlEqualTo("/some/thing"))
                .willReturn(ok("Hello Nomad!")));

        // WHEN
        Request request = new Request.Builder().url(wireMockRule.url("/some/thing")).build();
        String response = nomadApi.checkResponseAndGetBody(request);

        // THEN
        assertThat(response, is("Hello Nomad!"));
        assertThat(getFieldValue(nomadApi, "client"), notNullValue());
    }

    @Test
    public void testGETWithoutResponseMessage() {
        // GIVEN
        stubFor(get(urlEqualTo("/some/thing"))
                .willReturn(ok()));

        // WHEN
        Request request = new Request.Builder().url(wireMockRule.url("/some/thing")).build();
        String response = nomadApi.checkResponseAndGetBody(request);

        // THEN
        assertThat(response, notNullValue());
        assertThat(getFieldValue(nomadApi, "client"), notNullValue());
    }

    @Test
    public void testGET401() {
        // GIVEN
        stubFor(get(urlEqualTo("/some/thing"))
                .willReturn(aResponse()
                        .withStatus(401)));

        // WHEN
        Request request = new Request.Builder().url(wireMockRule.url("/some/thing")).build();
        String response = nomadApi.checkResponseAndGetBody(request);

        // THEN
        assertThat(response, notNullValue());
        assertThat(getFieldValue(nomadApi, "client"), nullValue());
    }

    @Test
    public void testGETUnknownHost() {
        // WHEN
        Request request = new Request.Builder().url("http://"+ UUID.randomUUID()).build();
        String response = nomadApi.checkResponseAndGetBody(request);

        // THEN
        assertThat(response, notNullValue());
        assertThat(getFieldValue(nomadApi, "client"), nullValue());
    }

    @Test
    public void testGETUnknownResource() {
        // GIVEN
        stubFor(get(urlEqualTo("/some/thing"))
                .willReturn(aResponse()
                        .withHeader("Content-Length", "0")
                        .withStatus(404)));

        // WHEN
        Request request = new Request.Builder().url(wireMockRule.url("/some/thing")).build();
        String response = nomadApi.checkResponseAndGetBody(request);

        // THEN
        assertThat(response, notNullValue());
        assertThat(getFieldValue(nomadApi, "client"), notNullValue());
    }

    @Test
    public void testPOST() {
        // GIVEN
        stubFor(post(urlEqualTo("/some/thing"))
                .willReturn(ok()));

        // WHEN
        Request request = new Request.Builder()
                .post(create("", MediaType.get("application/json")))
                .url(wireMockRule.url("/some/thing"))
                .build();
        String response = nomadApi.checkResponseAndGetBody(request);

        // THEN
        assertThat(response, notNullValue());
        assertThat(getFieldValue(nomadApi, "client"), notNullValue());
    }

}