package com.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

@DynamoDBTable(tableName = "omnitrack-mobilehub-1262440094-Trackers")

public class TrackersDO {
    private String _userId;
    private String _trackerId;
    private Double _color;
    private Boolean _isOnShortcut;
    private Double _position;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBIndexHashKey(attributeName = "userId", globalSecondaryIndexName = "sort_by_position")
    public String getUserId() {
        return _userId;
    }

    public void setUserId(final String _userId) {
        this._userId = _userId;
    }

    @DynamoDBRangeKey(attributeName = "trackerId")
    @DynamoDBAttribute(attributeName = "trackerId")
    public String getTrackerId() {
        return _trackerId;
    }

    public void setTrackerId(final String _trackerId) {
        this._trackerId = _trackerId;
    }

    @DynamoDBAttribute(attributeName = "color")
    public Double getColor() {
        return _color;
    }

    public void setColor(final Double _color) {
        this._color = _color;
    }

    @DynamoDBAttribute(attributeName = "is_on_shortcut")
    public Boolean getIsOnShortcut() {
        return _isOnShortcut;
    }

    public void setIsOnShortcut(final Boolean _isOnShortcut) {
        this._isOnShortcut = _isOnShortcut;
    }

    @DynamoDBIndexRangeKey(attributeName = "position", globalSecondaryIndexName = "sort_by_position")
    public Double getPosition() {
        return _position;
    }

    public void setPosition(final Double _position) {
        this._position = _position;
    }

}
