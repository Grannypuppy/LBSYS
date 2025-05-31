package jsonHandlers;

public class CardIDJson {
    private String action;
    private int cardID;

    public CardIDJson() {}

    public CardIDJson(String action, int cardID) {
        this.action = action;
        this.cardID = cardID;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getCardID() {
        return cardID;
    }

    public void setCardID(int cardID) {
        this.cardID = cardID;
    }
}
