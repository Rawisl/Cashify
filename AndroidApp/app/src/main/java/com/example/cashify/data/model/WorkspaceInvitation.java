package com.example.cashify.data.model;

import com.google.firebase.firestore.Exclude;

public class WorkspaceInvitation {
    @Exclude
    private String id;
    private String workspaceId;
    private String workspaceName;
    private String inviterName;
    private String inviterAvatar;
    private long timestamp;

    public WorkspaceInvitation() {}

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getWorkspaceName() { return workspaceName; }
    public void setWorkspaceName(String workspaceName) { this.workspaceName = workspaceName; }

    public String getInviterName() { return inviterName; }
    public void setInviterName(String inviterName) { this.inviterName = inviterName; }

    public String getInviterAvatar() { return inviterAvatar; }
    public void setInviterAvatar(String inviterAvatar) { this.inviterAvatar = inviterAvatar; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}