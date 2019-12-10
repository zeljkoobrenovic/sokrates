/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.search;

public class SearchRequest {
    private SearchExpression pathSearchExpression = new SearchExpression("");
    private SearchExpression contentSearchExpression = new SearchExpression("");

    public SearchRequest() {
    }

    public SearchRequest(SearchExpression pathSearchExpression, SearchExpression contentSearchExpression) {
        this.pathSearchExpression = pathSearchExpression;
        this.contentSearchExpression = contentSearchExpression;
    }

    public SearchExpression getPathSearchExpression() {
        return pathSearchExpression;
    }

    public void setPathSearchExpression(SearchExpression pathSearchExpression) {
        this.pathSearchExpression = pathSearchExpression;
    }

    public SearchExpression getContentSearchExpression() {
        return contentSearchExpression;
    }

    public void setContentSearchExpression(SearchExpression contentSearchExpression) {
        this.contentSearchExpression = contentSearchExpression;
    }
}
