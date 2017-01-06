package com.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.util.Map;

@DynamoDBTable(tableName = "omnitrack-mobilehub-1262440094-Items")

public class ItemsDO {
    private String _userId;
    private String _itemId;
    private Double _loggedAt;
    private Double _source;
    private String _trackerId;
    private Double _updateAt;
    private Map<String, String> _valueTable;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBIndexHashKey(attributeName = "userId", globalSecondaryIndexName = "DateSorted")
    public String getUserId() {
        return _userId;
    }

    public void setUserId(final String _userId) {
        this._userId = _userId;
    }

    @DynamoDBRangeKey(attributeName = "itemId")
    @DynamoDBAttribute(attributeName = "itemId")
    public String getItemId() {
        return _itemId;
    }

    public void setItemId(final String _itemId) {
        this._itemId = _itemId;
    }

    @DynamoDBIndexRangeKey(attributeName = "logged_at", globalSecondaryIndexNames = {"DateSorted", "OfTracker",})
    public Double getLoggedAt() {
        return _loggedAt;
    }

    public void setLoggedAt(final Double _loggedAt) {
        this._loggedAt = _loggedAt;
    }

    @DynamoDBAttribute(attributeName = "source")
    public Double getSource() {
        return _source;
    }

    public void setSource(final Double _source) {
        this._source = _source;
    }

    @DynamoDBIndexHashKey(attributeName = "trackerId", globalSecondaryIndexName = "OfTracker")
    public String getTrackerId() {
        return _trackerId;
    }

    public void setTrackerId(final String _trackerId) {
        this._trackerId = _trackerId;
    }

    @DynamoDBAttribute(attributeName = "update_at")
    public Double getUpdateAt() {
        return _updateAt;
    }

    public void setUpdateAt(final Double _updateAt) {
        this._updateAt = _updateAt;
    }

    @DynamoDBAttribute(attributeName = "value_table")
    public Map<String, String> getValueTable() {
        return _valueTable;
    }

    public void setValueTable(final Map<String, String> _valueTable) {
        this._valueTable = _valueTable;
    }

}
