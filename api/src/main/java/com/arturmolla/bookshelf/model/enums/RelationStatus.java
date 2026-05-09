package com.arturmolla.bookshelf.model.enums;

public enum RelationStatus {

    /** Friend request sent and waiting for a response. */
    PENDING,

    /** Friend request was accepted – both users are friends. */
    ACCEPTED,

    /** Friend request was rejected by the addressee. */
    REJECTED
}

