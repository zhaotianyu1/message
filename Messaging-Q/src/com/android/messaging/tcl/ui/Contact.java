package com.android.messaging.tcl.ui;

public class Contact {


    long dataId;
    long contactId;
    String lookupKey;
    String displayName;
    String photoThumbnailUri;
    String destination;
    int destinationType;
    String destinationLabel;

    public Contact(String name, String destination ) {
        this.displayName = name;
        this.destination = destination;
    }

    public long getDataId() {
        return dataId;
    }

    public void setDataId(long dataId) {
        this.dataId = dataId;
    }

    public long getContactId() {
        return contactId;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public void setLookupKey(String lookupKey) {
        this.lookupKey = lookupKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoThumbnailUri() {
        return photoThumbnailUri;
    }

    public void setPhotoThumbnailUri(String photoThumbnailUri) {
        this.photoThumbnailUri = photoThumbnailUri;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(int destinationType) {
        this.destinationType = destinationType;
    }

    public String getDestinationLabel() {
        return destinationLabel;
    }

    public void setDestinationLabel(String destinationLabel) {
        this.destinationLabel = destinationLabel;
    }
}
