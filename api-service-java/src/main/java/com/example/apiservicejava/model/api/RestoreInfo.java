package com.example.apiservicejava.model.api;

public class RestoreInfo {

    private String mode;
    private String status;
    private String strategy;
    private boolean liveSessionAvailable;
    private boolean snapshotUsed;
    private boolean readOnly;
    private String message;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public boolean isLiveSessionAvailable() {
        return liveSessionAvailable;
    }

    public void setLiveSessionAvailable(boolean liveSessionAvailable) {
        this.liveSessionAvailable = liveSessionAvailable;
    }

    public boolean isSnapshotUsed() {
        return snapshotUsed;
    }

    public void setSnapshotUsed(boolean snapshotUsed) {
        this.snapshotUsed = snapshotUsed;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}