package demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MainAppMockWebServerTests {

    private static MockWebServer mockWebServer;

    private static final Dispatcher dispatcher = new Dispatcher() {
        @Override
        public MockResponse dispatch(RecordedRequest request) {
            MockResponse mockResponse = new MockResponse();
            mockResponse.addHeader("Content-Type", "application/json");
            switch (Objects.requireNonNull(request.getPath())) {
                case "/person/1?delay=2":
                    return mockResponse
                            .setResponseCode(200)
                            .setBody("{\n" +
                                    "  \"id\": 1,\n" +
                                    "  \"name\": \"Amanda\"\n" +
                                    "}");
                case "/person/2?delay=2":
                    return mockResponse.setResponseCode(500);
                case "/person/3?delay=2":
                    return mockResponse.setResponseCode(200).setBody("{\"id\": 1, \"name\":\"duke\"}");
            }
            return mockResponse.setResponseCode(404);
        }
    };

    @Autowired
    WebTestClient client;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) throws IOException {
        r.add("remote-services.base-url", () -> "http://localhost:" + mockWebServer.getPort());
    }

    @BeforeAll
    static void beforeClass() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);
        mockWebServer.start();
    }

    @Test
    void person() throws InterruptedException {
        this.client.get().uri("/person/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Person.class).isEqualTo(new Person(1L, "Amanda"));

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/person/1?delay=2", recordedRequest.getPath());
    }
}
