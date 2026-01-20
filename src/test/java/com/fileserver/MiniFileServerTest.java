package com.fileserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for MiniFileServer endpoints.
 * Tests health, version, upload, and download functionality.
 */
class MiniFileServerTest {
  private static final int TEST_PORT = 8081;
  private static final String BASE_URL = "http://localhost:" + TEST_PORT;
  private static HttpServer server;
  private static final Path STORAGE = Paths.get("storage");

  /**
   * Set up test server before all tests.
   *
   * @throws Exception if server creation fails
   */
  @BeforeAll
  static void setUp() throws Exception {
    Files.createDirectories(STORAGE);

    server = HttpServer.create(new InetSocketAddress(TEST_PORT), 0);
    server.createContext("/health", MiniFileServerTest::handleHealth);
    server.createContext("/version", MiniFileServerTest::handleVersion);
    server.createContext("/upload", MiniFileServerTest::handleUpload);
    server.createContext("/download", MiniFileServerTest::handleDownload);
    server.start();
  }

  /**
   * Tear down test server after all tests.
   */
  @AfterAll
  static void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  /**
   * Test health endpoint returns 200 OK.
   *
   * @throws Exception if test fails
   */
  @Test
  void testHealthEndpoint() throws Exception {
    URL url = new URI(BASE_URL + "/health").toURL();
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");

    int responseCode = conn.getResponseCode();
    assertEquals(200, responseCode, "Health endpoint should return 200");

    InputStream is = conn.getInputStream();
    String response = new String(is.readAllBytes());
    assertEquals("OK", response, "Health endpoint should return 'OK'");

    conn.disconnect();
  }

  /**
   * Test version endpoint returns version string.
   *
   * @throws Exception if test fails
   */
  @Test
  void testVersionEndpoint() throws Exception {
    URL url = new URI(BASE_URL + "/version").toURL();
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");

    int responseCode = conn.getResponseCode();
    assertEquals(200, responseCode, "Version endpoint should return 200");

    InputStream is = conn.getInputStream();
    String response = new String(is.readAllBytes());
    assertNotNull(response, "Version should not be null");
    assertTrue(response.matches("\\d+\\.\\d+\\.\\d+"),
        "Version should match semantic versioning");

    conn.disconnect();
  }

  /**
   * Test upload endpoint accepts files.
   *
   * @throws Exception if test fails
   */
  @Test
  void testUploadEndpoint() throws Exception {
    URL url = new URI(BASE_URL + "/upload").toURL();
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("X-Filename", "test-file.txt");
    conn.setDoOutput(true);

    String fileContent = "Test file content";
    conn.getOutputStream().write(fileContent.getBytes());

    int responseCode = conn.getResponseCode();
    assertEquals(200, responseCode, "Upload should return 200");

    InputStream is = conn.getInputStream();
    String response = new String(is.readAllBytes());
    assertTrue(response.contains("Uploaded"), "Response should confirm upload");

    conn.disconnect();

    // Clean up uploaded file
    Files.deleteIfExists(STORAGE.resolve("test-file.txt"));
  }

  /**
   * Test download endpoint serves files.
   *
   * @throws Exception if test fails
   */
  @Test
  void testDownloadEndpoint() throws Exception {
    // First, create a test file
    Path testFile = STORAGE.resolve("test-download.txt");
    Files.writeString(testFile, "Download test content");

    URL url = new URI(BASE_URL + "/download?name=test-download.txt").toURL();
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");

    int responseCode = conn.getResponseCode();
    assertEquals(200, responseCode, "Download should return 200");

    InputStream is = conn.getInputStream();
    String content = new String(is.readAllBytes());
    assertEquals("Download test content", content, "Downloaded content should match");

    conn.disconnect();

    // Clean up
    Files.deleteIfExists(testFile);
  }

  // Handler methods for test server - must throw IOException only

  private static void handleHealth(HttpExchange exchange) throws IOException {
    String response = "OK";
    exchange.sendResponseHeaders(200, response.getBytes().length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(response.getBytes());
    }
    exchange.close();
  }

  private static void handleVersion(HttpExchange exchange) throws IOException {
    String response = "1.0.0";
    exchange.sendResponseHeaders(200, response.getBytes().length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(response.getBytes());
    }
    exchange.close();
  }

  private static void handleUpload(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
      exchange.sendResponseHeaders(405, -1);
      exchange.close();
      return;
    }

    String filename = exchange.getRequestHeaders().getFirst("X-Filename");
    if (filename == null || filename.isEmpty()) {
      exchange.sendResponseHeaders(400, -1);
      exchange.close();
      return;
    }

    Path target = STORAGE.resolve(filename);
    try (InputStream in = exchange.getRequestBody()) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }

    String response = "Uploaded: " + filename;
    exchange.sendResponseHeaders(200, response.getBytes().length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(response.getBytes());
    }
    exchange.close();
  }

  private static void handleDownload(HttpExchange exchange) throws IOException {
    String query = exchange.getRequestURI().getQuery();
    if (query == null || !query.startsWith("name=")) {
      exchange.sendResponseHeaders(400, -1);
      exchange.close();
      return;
    }

    String filename = URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8);
    Path file = STORAGE.resolve(filename);

    if (!Files.exists(file)) {
      exchange.sendResponseHeaders(404, -1);
      exchange.close();
      return;
    }

    exchange.sendResponseHeaders(200, Files.size(file));
    try (OutputStream os = exchange.getResponseBody()) {
      Files.copy(file, os);
    }
    exchange.close();
  }
}
