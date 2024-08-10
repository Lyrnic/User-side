package com.lyrnic.userside.model;

import org.json.JSONException;
import org.json.JSONObject;

public class SmsMessage {
    private String id;
    private String address;
    private String person;
    private long date;
    private String body;
    private int type;

    // Getters and setters

    public SmsMessage(String id, String address, String person, long date, String body, int type) {
        this.id = id;
        this.address = address;
        this.person = person;
        this.date = date;
        this.body = body;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPerson() { return person; }
    public void setPerson(String person) { this.person = person; }
    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public void setType(int type){
        this.type = type;
    }
    public int getType(){
        return type;
    }
}
