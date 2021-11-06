package nl.obren.sokrates.sourcecode.landscape;

public class WebFrameLink {
    // An iFrame title (header)
    private String title = "";

    // An iFrame source
    private String src = "";

    // An iFrame CSS-defined style
    private String style = "";

    // If true, iFrame will allow scrolling
    private Boolean scrolling = true;

    // An optional link to external page with more info (will go there on the click on the header)
    private String moreInfoLink = "";

    public WebFrameLink() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getMoreInfoLink() {
        return moreInfoLink;
    }

    public void setMoreInfoLink(String moreInfoLink) {
        this.moreInfoLink = moreInfoLink;
    }

    public Boolean getScrolling() {
        return scrolling;
    }

    public void setScrolling(Boolean scrolling) {
        this.scrolling = scrolling;
    }
}
