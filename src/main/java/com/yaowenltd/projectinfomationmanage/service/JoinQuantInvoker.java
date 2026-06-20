/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaowenltd.projectinfomationmanage.config.QuantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Invokes the JoinQuant Python CLI as a child process and exposes a small
 * typed API surface. The Java backend intentionally avoids embedding a Python
 * interpreter - we spawn a short-lived python process per call, exchange a
 * single JSON message on stdin/stdout, and parse the response.
 */
@Service
public class JoinQuantInvoker {

    private static final Logger log = LoggerFactory.getLogger(JoinQuantInvoker.class);

    private final QuantProperties properties;

    private final ObjectMapper mapper = new ObjectMapper();

    public JoinQuantInvoker(QuantProperties properties) {
        this.properties = properties;
    }

    /**
     * Generic entry point. Returns the JSON `data` field as a {@link JsonNode}
     * or throws {@link JoinQuantInvocationException} on failure.
     */
    public JsonNode invoke(Map<String, Object> payload) {
        String mode = properties.getMode() == null ? "PROCESS" : properties.getMode().toUpperCase();
        if ("REMOTE".equals(mode)) {
            throw new JoinQuantInvocationException(
                "REMOTE mode is not bundled - wire a WebClient/RestTemplate call to "
                    + "quant.remote-base-url when you need to run jqdatasdk off-host.",
                "JQ_REMOTE_NOT_IMPLEMENTED");
        }
        return invokeProcess(payload);
    }

    // ---- convenience wrappers ----
    public JsonNode ping() {
        return invoke(mapOf("action", "ping"));
    }

    public JsonNode auth(String username, String password) {
        return invoke(mapOf("action", "auth", "username", username, "password", password));
    }

    public JsonNode getAccountInfo() {
        return invoke(mapOf("action", "get_account_info"));
    }

    public JsonNode getPositions(String security) {
        return invoke(mapOf("action", "get_positions", "security", security));
    }

    public JsonNode getOrders(String security) {
        return invoke(mapOf("action", "get_orders", "security", security));
    }

    public JsonNode getTrades(String security) {
        return invoke(mapOf("action", "get_trades", "security", security));
    }

    public JsonNode getSecurities(List<String> types, String date) {
        Map<String, Object> p = mapOf("action", "get_securities", "date", date);
        p.put("types", types);
        return invoke(p);
    }

    public JsonNode getPrice(String security, String startDate, String endDate, String frequency) {
        return invoke(mapOf(
            "action", "get_price",
            "security", security,
            "startDate", startDate,
            "endDate", endDate,
            "frequency", frequency == null ? "daily" : frequency
        ));
    }

    public JsonNode submitStrategy(String code, Map<String, Object> parameters, String runType,
                                   String startDate, String endDate, double initialCapital,
                                   String frequency, String benchmark) {
        return invoke(mapOf(
            "action", "submit_strategy",
            "code", code,
            "parameters", parameters == null ? Map.of() : parameters,
            "runType", runType,
            "startDate", startDate,
            "endDate", endDate,
            "initialCapital", initialCapital,
            "frequency", frequency,
            "benchmark", benchmark
        ));
    }

    public JsonNode getRunStatus(String runId) {
        return invoke(mapOf("action", "get_run_status", "runId", runId));
    }

    public JsonNode stopStrategy(String runId) {
        return invoke(mapOf("action", "stop_strategy", "runId", runId));
    }

    public JsonNode getMetrics(String runId) {
        return invoke(mapOf("action", "get_metrics", "runId", runId));
    }

    // ---- internals ----
    private JsonNode invokeProcess(Map<String, Object> payload) {
        ProcessBuilder pb = new ProcessBuilder(properties.getPythonBin(), properties.getCliScript());
        pb.redirectErrorStream(false);
        Process process = null;
        try {
            process = pb.start();
            String stdinJson = mapper.writeValueAsString(payload);
            try (var os = process.getOutputStream()) {
                os.write(stdinJson.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            String stdout;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line);
                }
                stdout = sb.toString();
            }
            String stderr;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                stderr = sb.toString();
            }
            boolean finished = process.waitFor(properties.getCallTimeoutSeconds(), java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new JoinQuantInvocationException("python process timed out after "
                    + properties.getCallTimeoutSeconds() + "s", "JQ_TIMEOUT", stderr);
            }
            if (process.exitValue() != 0 && stdout.isEmpty()) {
                throw new JoinQuantInvocationException("python process exited with code "
                    + process.exitValue(), "JQ_EXIT_NONZERO", stderr);
            }
            if (!stderr.isEmpty()) {
                log.debug("python stderr: {}", stderr);
            }
            JsonNode root = mapper.readTree(stdout);
            if (root.path("ok").asBoolean(false)) {
                return root.path("data");
            }
            throw new JoinQuantInvocationException(
                root.path("message").asText("unknown error"),
                root.path("code").asText("JQ_FAIL"),
                stderr);
        } catch (IOException ex) {
            throw new JoinQuantInvocationException("I/O error talking to python: " + ex.getMessage(),
                "JQ_IO_ERROR", "", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new JoinQuantInvocationException("interrupted while waiting for python",
                "JQ_INTERRUPTED", "", ex);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static Map<String, Object> mapOf(Object... kv) {
        if ((kv.length & 1) != 0) {
            throw new IllegalArgumentException("mapOf requires even number of args");
        }
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
