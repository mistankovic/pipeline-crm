package com.pipelinecrm.architecture;

/**
 * The layer names and package patterns every architecture rule is written against, in one
 * place, so that a rule cannot quietly disagree with another rule about what "domain" means.
 */
final class Layers {

    static final String ROOT = "com.pipelinecrm";

    static final String DOMAIN = "Domain";
    static final String APPLICATION = "Application";
    static final String ADAPTER_WEB = "Web adapter";
    static final String ADAPTER_PERSISTENCE = "Persistence adapter";
    static final String BOOTSTRAP = "Bootstrap";

    static final String DOMAIN_PACKAGES = "com.pipelinecrm.domain..";
    static final String APPLICATION_PACKAGES = "com.pipelinecrm.application..";
    static final String ADAPTER_WEB_PACKAGES = "com.pipelinecrm.adapter.web..";
    static final String ADAPTER_PERSISTENCE_PACKAGES = "com.pipelinecrm.adapter.persistence..";
    static final String BOOTSTRAP_PACKAGES = "com.pipelinecrm.bootstrap..";

    private Layers() {
    }
}
