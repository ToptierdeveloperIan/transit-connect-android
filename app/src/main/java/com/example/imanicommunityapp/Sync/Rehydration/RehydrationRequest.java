// java
package com.example.imanicommunityapp.Sync.Rehydration;

import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class RehydrationRequest {
    private final RehydrationScopeTags scope;
    private final RehydrationTrigger trigger;
    private final RehydrationPriority priority;
    private final boolean forceFresh;
    private final long requestedAt;
    private final List<String> affectedIds; // optional list of IDs for CUSTOM or targeted rehydrate
    private final String correlationId;

    public RehydrationRequest(
            RehydrationScopeTags scope,
            RehydrationTrigger trigger,
            RehydrationPriority priority,
            boolean forceFresh,
            @Nullable List<String> affectedIds,
            @Nullable String correlationId
    ) {
        this.scope = scope;
        this.trigger = trigger;
        this.priority = priority == null ? RehydrationPriority.MEDIUM : priority;
        this.forceFresh = forceFresh;
        this.requestedAt = System.currentTimeMillis();
        this.affectedIds = affectedIds == null ? Collections.emptyList() : affectedIds;
        this.correlationId = correlationId == null ? UUID.randomUUID().toString() : correlationId;
    }

    public RehydrationScopeTags getScope() { return scope; }
    public RehydrationTrigger getTrigger() { return trigger; }
    public RehydrationPriority getPriority() { return priority; }
    public boolean isForceFresh() { return forceFresh; }
    public long getRequestedAt() { return requestedAt; }
    public List<String> getAffectedIds() { return affectedIds; }
    public String getCorrelationId() { return correlationId; }
}
