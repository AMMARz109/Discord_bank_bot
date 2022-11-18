package me.ammardev;

public class BankMember {

    String id;
    long bal;

    public BankMember(String id, long bal) {
        this.id = id;
        this.bal = bal;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getBal() {
        return bal;
    }

    public void setBal(long bal) {
        this.bal = bal;
    }
}
