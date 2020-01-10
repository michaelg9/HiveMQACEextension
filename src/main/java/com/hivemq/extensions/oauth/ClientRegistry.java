package com.hivemq.extensions.oauth;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRegistry {
    private final Map<String, IntrospectionResponse> map = new ConcurrentHashMap<>();

    public void addClient(@NotNull final String id, @NotNull final IntrospectionResponse response) {
        map.put(id, response);
    }

    public IntrospectionResponse removeClient(@NotNull final String id) {
        return map.remove(id);
    }

    public boolean isClientAuthenticated(@NotNull final String id) {
        boolean isAuthenticated = false;
        if (map.containsKey(id)) {
            IntrospectionResponse response = map.get(id);
            isAuthenticated = response.getExp() > System.currentTimeMillis();
            if (!isAuthenticated) removeClient(id);
        }
        return isAuthenticated;
    }

    public TopicPermission getClientPermissions(@NotNull final String id) {
        if (!map.containsKey(id)) return null;
        IntrospectionResponse response = map.get(id);
        String topicFilter = response.getAud();
        TopicPermission.MqttActivity permission = response.parseScope();
        return Builders.topicPermission()
                .topicFilter(topicFilter)
                .qos(TopicPermission.Qos.ALL)
                .activity(permission)
                .type(TopicPermission.PermissionType.ALLOW)
                .retain(TopicPermission.Retain.ALL)
                .build();
    }
}
