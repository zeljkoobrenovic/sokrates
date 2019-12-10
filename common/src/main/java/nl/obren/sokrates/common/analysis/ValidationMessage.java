/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.analysis;

public class ValidationMessage {
    private String message;
    private String searchPhrase = "";

    public ValidationMessage() {
    }

    public ValidationMessage(String message) {
        this.message = message;
    }

    public ValidationMessage(String message, String searchPhrase) {
        this.message = message;
        this.searchPhrase = searchPhrase;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSearchPhrase() {
        return searchPhrase;
    }

    public void setSearchPhrase(String searchPhrase) {
        this.searchPhrase = searchPhrase;
    }
}
