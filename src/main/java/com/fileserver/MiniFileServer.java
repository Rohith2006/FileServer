package com.fileserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Production-grade file server with health and version endpoints.
 * Provides file upload/download capabilities with proper error handling.
 */
public class MiniFileServer {
  private static final Path STORAGE = Paths.get("storage");
  private static final String VERSION = "1.0.0";
  private static HttpServer server;

  /**
   * Main entry point for the file server.
   *
   * @param args command line arguments (unused)
   * @throws Exception if server creation fails
   */
  public static void main(String[] args) throws Exception {
    Files.createDirectories(STORAGE);

    server = HttpServer.create(new InetSocketAddress(8080), 0);
    server.createContext("/health", MiniFileServer::handleHealth);
    server.createContext("/version", MiniFileServer::handleVersion);
    server.createContext("/upload", MiniFileServer::handleUpload);
    server.createContext("/download", MiniFileServer::handleDownload);

    // Graceful shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutting down server...");
      server.stop(0);
    }));

    server.start();
    System.out.println("Server running at http://localhost:8080");
    System.out.println("Version: " + VERSION);
  }

  /**
   * Health check endpoint - returns OK if server is running.
   *
   * @param exchange HTTP exchange object
   * @throws IOException if response cannot be sent
   */
  private static void handleHealth(HttpExchange exchange) throws IOException {
    String response = "OK";
    exchange.sendResponseHeaders(200, response.getBytes().length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(response.getBytes());
    }
    exchange.close();
  }

  /**
   * Version endpoint - returns application version.
   *
   * @param exchange HTTP exchange object
   * @throws IOException if response cannot be sent
   */
  private static void handleVersion(HttpExchange exchange) throws IOException {
    String response = VERSION;
    exchange.getResponseHeaders().set("Content-Type", "text/plain");
    exchange.sendResponseHeaders(200, response.getBytes().length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(response.getBytes());
    }
    exchange.close();
  }

  /**
   * Upload endpoint - accepts file uploads via POST with X-Filename header.
   *
   * @param exchange HTTP exchange object
   * @throws IOException if file cannot be saved
   */
  private static void handleUpload(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
      exchange.sendResponseHeaders(405, -1);
      exchange.close();
      return;
    }

    try {
      String filename = exchange.getRequestHeaders().getFirst("X-Filename");

      if (filename == null || filename.isEmpty()) {
        String errorResponse = "Missing X-Filename header";
        exchange.sendResponseHeaders(400, errorResponse.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(errorResponse.getBytes());
        }
        exchange.close();
        return;
      }

      // Security: Prevent directory traversal
      filename = Paths.get(filename).getFileName().toString();

      Path target = STORAGE.resolve(filename);
      try (InputStream in = exchange.getRequestBody()) {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
      }

      String response = "Uploaded: " + filename;
      exchange.sendResponseHeaders(200, response.getBytes().length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes());
      }
    } catch (Exception e) {
      e.printStackTrace();
      String errorResponse = "Upload failed: " + e.getMessage();
      exchange.sendResponseHeaders(500, errorResponse.getBytes().length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(errorResponse.getBytes());
      }
    } finally {
      exchange.close();
    }
  }

  /**
   * Download endpoint - serves files by name parameter.
   *
   * @param exchange HTTP exchange object
   * @throws IOException if file cannot be read
   */
  private static void handleDownload(HttpExchange exchange) throws IOException {
    String query = exchange.getRequestURI().getQuery();
    if (query == null || !query.startsWith("name=")) {
      exchange.sendResponseHeaders(400, -1);
      exchange.close();
      return;
    }

    String filename = URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8);

    // Security: Prevent directory traversal
    filename = Paths.get(filename).getFileName().toString();

    Path file = STORAGE.resolve(filename);

    if (!Files.exists(file)) {
      exchange.sendResponseHeaders(404, -1);
      exchange.close();
      return;
    }

    String contentType = Files.probeContentType(file);
    if (contentType == null) {
      contentType = "application/octet-stream";
    }

    exchange.getResponseHeaders().add("Content-Disposition",
        "attachment; filename=\"" + filename + "\"");
    exchange.getResponseHeaders().add("Content-Type", contentType);
    exchange.sendResponseHeaders(200, Files.size(file));

    try (OutputStream os = exchange.getResponseBody()) {
      Files.copy(file, os);
    }
    exchange.close();
  }
}
