package models;

public class BetterTranslationForm {

    private String language;
    private String oldText;
    private String betterText;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getOldText() {
        return oldText;
    }

    public void setOldText(String oldText) {
        this.oldText = oldText;
    }

    public String getBetterText() {
        return betterText;
    }

    public void setBetterText(String betterText) {
        this.betterText = betterText;
    }
}
