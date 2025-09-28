package com.evstation.ev_charging_backend.enums;

public enum Role {

    USER,
    HOST,
    ADMIN,
    PENDING_HOST;

    Role valueOf(Role role) {
        throw new UnsupportedOperationException("Unimplemented method 'valueOf'");
    }
}
