package com.dezhik.conf.loader;

import com.dezhik.conf.client.ConfValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RemoteUpdatesLoader implements UpdatesLoader {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final String url;
    private final String host;
    private final ObjectMapper mapper = new ObjectMapper();

    public RemoteUpdatesLoader(String url, String host) {
        this.url = url;
        this.host = host;
    }

    @Override
    public Map<String, ConfValues> getUpdates(List<String> modules, long version) throws IOException {
        HttpClientBuilder builder = HttpClients.custom().setConnectionTimeToLive(10, TimeUnit.SECONDS);
        CloseableHttpClient httpclient = builder.build();

        HttpPost request = new HttpPost(url);

        request.setEntity(EntityBuilder.create()
                .setText("{\"host\": \"" + host + "\", \"modules\":[\""+ String.join(",", modules) +"\"], \"version\":"+version+"}")
                .build()
        );

        Map<String, ConfValues> result = new HashMap<>();
        // can redirect and status would be for Location header's page

        CloseableHttpResponse response = httpclient.execute(request);
        final HttpEntity responseEntity = response.getEntity();

//            JsonNode rootNode = mapper.readTree(responseEntity.getContent());
//            if (rootNode.isEmpty()) {
//                log.warn("Empty response");
//                // todo reschedule
//                return Collections.emptyMap();
//            }

        if (response.getStatusLine().getStatusCode() != 200) {
            log.error("Conf loading failed with " + response.getStatusLine().getStatusCode() + " status code.");
            return Collections.emptyMap();
        }

        UpdatesResponse updates = mapper.readValue(responseEntity.getContent(), UpdatesResponse.class);

        if (updates.error != null) {
            log.error("Conf loading error: {}", updates.error);
            return Collections.emptyMap();
        }

        for (UpdatesResponse.Module module : updates.modules) {
            final Map<String, String> newProperties = new HashMap<>();
            final List<String> deletions = new ArrayList<>();

            for (UpdatesResponse.Module.Entry entry : module.properties) {
                if (entry.value == null) {
                    deletions.add(entry.name);
                    continue;
                }

                newProperties.put(entry.name, entry.value);
            }

            result.put(
                module.name,
                new ConfValues(
                    newProperties,
                    deletions,
                    module.lastVersion
                )
            );
        }

        return result;
    }
}
