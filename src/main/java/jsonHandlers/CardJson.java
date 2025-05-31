package jsonHandlers;

public class CardJson {
    private String action;
    private int cardId;
    private String name;
    private String department;
    private String type;

    public CardJson() {}

    public CardJson(String action, int cardId, String name, String department, String type) {
        this.action = action;
        this.cardId = cardId;
        this.name = name;
        this.department = department;
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
