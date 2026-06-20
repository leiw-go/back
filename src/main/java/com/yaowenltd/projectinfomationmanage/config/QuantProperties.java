/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the JoinQuant integration.
 */
@Configuration
@ConfigurationProperties(prefix = "quant")
public class QuantProperties {

    /** PROCESS = spawn python script per call, REMOTE = talk to HTTP shim. */
    private String mode = "PROCESS";

    private String pythonBin = "python3";

    private String cliScript = "./quant/scripts/jq_cli.py";

    private String remoteBaseUrl = "http://127.0.0.1:5000";

    private String remoteToken = "";

    private int callTimeoutSeconds = 60;

    private Aes aes = new Aes();

    public static class Aes {
        /**
         * AES key, 16/24/32 bytes. Empty by default - {@link com.yaowenltd.projectinfomationmanage.common.AesUtil}
         * will refuse to start until this is set via env var {@code QUANT_AES_KEY} or
         * {@code quant.aes.key} in application.yml. NEVER commit a real key.
         */
        private String key = "";

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getPythonBin() {
        return pythonBin;
    }

    public void setPythonBin(String pythonBin) {
        this.pythonBin = pythonBin;
    }

    public String getCliScript() {
        return cliScript;
    }

    public void setCliScript(String cliScript) {
        this.cliScript = cliScript;
    }

    public String getRemoteBaseUrl() {
        return remoteBaseUrl;
    }

    public void setRemoteBaseUrl(String remoteBaseUrl) {
        this.remoteBaseUrl = remoteBaseUrl;
    }

    public String getRemoteToken() {
        return remoteToken;
    }

    public void setRemoteToken(String remoteToken) {
        this.remoteToken = remoteToken;
    }

    public int getCallTimeoutSeconds() {
        return callTimeoutSeconds;
    }

    public void setCallTimeoutSeconds(int callTimeoutSeconds) {
        this.callTimeoutSeconds = callTimeoutSeconds;
    }

    public Aes getAes() {
        return aes;
    }

    public void setAes(Aes aes) {
        this.aes = aes;
    }
}
