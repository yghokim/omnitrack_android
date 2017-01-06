package com.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.util.Map;

@DynamoDBTable(tableName = "omnitrack-mobilehub-1262440094-Attributes")

public class AttributesDO {
    private String _userId;
    private String _attributeId;
    private String _connection;
    private Boolean _isRequired;
    private String _name;
    private Double _position;
    private Map<String, String> _properties;
    private String _trackerId;
    private String _type;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBAttribute(attributeName = "userId")
    public String getUserId() {
        return _userId;
    }

    public void setUserId(final String _userId) {
        this._userId = _userId;
    }

    @DynamoDBRangeKey(attributeName = "attributeId")
    @DynamoDBAttribute(attributeName = "attributeId")
    public String getAttributeId() {
        return _attributeId;
    }

    public void setAttributeId(final String _attributeId) {
        this._attributeId = _attributeId;
    }

    @DynamoDBAttribute(attributeName = "connection")
    public String getConnection() {
        return _connection;
    }

    public void setConnection(final String _connection) {
        this._connection = _connection;
    }

    @DynamoDBAttribute(attributeName = "is_required")
    public Boolean getIsRequired() {
        return _isRequired;
    }

    public void setIsRequired(final Boolean _isRequired) {
        this._isRequired = _isRequired;
    }

    @DynamoDBAttribute(attributeName = "name")
    public String getName() {
        return _name;
    }

    public void setName(final String _name) {
        this._name = _name;
    }

    @DynamoDBIndexRangeKey(attributeName = "position", globalSecondaryIndexName = "tracker_ordered")
    public Double getPosition() {
        return _position;
    }

    public void setPosition(final Double _position) {
        this._position = _position;
    }

    @DynamoDBAttribute(attributeName = "properties")
    public Map<String, String> getProperties() {
        return _properties;
    }

    public void setProperties(final Map<String, String> _properties) {
        this._properties = _properties;
    }

    @DynamoDBIndexHashKey(attributeName = "trackerId", globalSecondaryIndexName = "tracker_ordered")
    public String getTrackerId() {
        return _trackerId;
    }

    public void setTrackerId(final String _trackerId) {
        this._trackerId = _trackerId;
    }

    @DynamoDBAttribute(attributeName = "type")
    public String getType() {
        return _type;
    }

    public void setType(final String _type) {
        this._type = _type;
    }

}
