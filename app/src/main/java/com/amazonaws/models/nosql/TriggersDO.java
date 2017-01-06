package com.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

@DynamoDBTable(tableName = "omnitrack-mobilehub-1262440094-Triggers")

public class TriggersDO {
    private String _userId;
    private String _triggerId;
    private Double _action;
    private Boolean _isOn;
    private Double _lastTriggeredTimestamp;
    private String _name;
    private Double _position;
    private String _trackerId;
    private Double _type;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBAttribute(attributeName = "userId")
    public String getUserId() {
        return _userId;
    }

    public void setUserId(final String _userId) {
        this._userId = _userId;
    }

    @DynamoDBRangeKey(attributeName = "triggerId")
    @DynamoDBAttribute(attributeName = "triggerId")
    public String getTriggerId() {
        return _triggerId;
    }

    public void setTriggerId(final String _triggerId) {
        this._triggerId = _triggerId;
    }

    @DynamoDBAttribute(attributeName = "action")
    public Double getAction() {
        return _action;
    }

    public void setAction(final Double _action) {
        this._action = _action;
    }

    @DynamoDBAttribute(attributeName = "is_on")
    public Boolean getIsOn() {
        return _isOn;
    }

    public void setIsOn(final Boolean _isOn) {
        this._isOn = _isOn;
    }

    @DynamoDBAttribute(attributeName = "last_triggered_timestamp")
    public Double getLastTriggeredTimestamp() {
        return _lastTriggeredTimestamp;
    }

    public void setLastTriggeredTimestamp(final Double _lastTriggeredTimestamp) {
        this._lastTriggeredTimestamp = _lastTriggeredTimestamp;
    }

    @DynamoDBAttribute(attributeName = "name")
    public String getName() {
        return _name;
    }

    public void setName(final String _name) {
        this._name = _name;
    }

    @DynamoDBIndexRangeKey(attributeName = "position", globalSecondaryIndexName = "positionSorted")
    public Double getPosition() {
        return _position;
    }

    public void setPosition(final Double _position) {
        this._position = _position;
    }

    @DynamoDBIndexHashKey(attributeName = "trackerId", globalSecondaryIndexName = "positionSorted")
    public String getTrackerId() {
        return _trackerId;
    }

    public void setTrackerId(final String _trackerId) {
        this._trackerId = _trackerId;
    }

    @DynamoDBAttribute(attributeName = "type")
    public Double getType() {
        return _type;
    }

    public void setType(final Double _type) {
        this._type = _type;
    }

}
