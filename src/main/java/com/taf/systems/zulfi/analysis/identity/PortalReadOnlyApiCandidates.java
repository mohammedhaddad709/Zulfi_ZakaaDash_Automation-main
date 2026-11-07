package com.taf.systems.zulfi.analysis.identity;

import com.taf.systems.zulfi.config.PortalConfig;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Heuristic read-only GET URLs (session cookie required). */
public final class PortalReadOnlyApiCandidates {

    private PortalReadOnlyApiCandidates() {}

    public static List<String> analysisDetailEndpoints(long analysisId) {
        Set<String> urls = new LinkedHashSet<>();
        URI vueBase = URI.create(PortalConfig.baseUrl());
        String vuePath = vueBase.getPath() == null ? "/" : vueBase.getPath();
        String apiPath = vuePath.replace("-portal-vue/", "-portal-api/").replace("-portal-vue", "-portal-api");
        if (!apiPath.endsWith("/")) {
            apiPath = apiPath + "/";
        }
        String origin = vueBase.getScheme() + "://" + vueBase.getHost() + (vueBase.getPort() > 0 ? ":" + vueBase.getPort() : "");
        String apiRoot = origin + apiPath;
        urls.add(apiRoot + "analyses/" + analysisId);
        urls.add(apiRoot + "analysis/" + analysisId);
        urls.add(apiRoot + "api/analyses/" + analysisId);
        urls.add(apiRoot + "api/v1/analyses/" + analysisId);
        urls.add(origin + vuePath + "api/analyses/" + analysisId);
        return new ArrayList<>(urls);
    }
}
