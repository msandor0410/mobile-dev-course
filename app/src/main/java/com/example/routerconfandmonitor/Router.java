package com.example.routerconfandmonitor;

public class Router {
    private String id;
    private String name;
    private String ip;
    private String username;
    private String password;
    private String creatorId; // ÚJ mező

    public Router() {} // Firestore-hoz kell üres konstruktor

    public Router(String name, String ip, String username, String password, String creatorId) {
        this.name = name;
        this.ip = ip;
        this.username = username;
        this.password = password;
        this.creatorId = creatorId;
    }

    // Getters + Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public String getIp() { return ip; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getCreatorId() { return creatorId; }
}
