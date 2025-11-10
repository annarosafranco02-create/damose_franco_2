package it.damose.controller;


public interface ConnectionListener {
    void onConnectionStatusChanged(boolean isOnline, boolean isFirstCheck);
}