package org.musicStore.model;

public class Parts extends Stock {
    protected String partType;

    public Parts() { super(); }

    public Parts(String name, String brand, double price,
                 int quantity, String category, String partType) {
        super(name, brand, price, quantity, category);
        this.partType = partType;
    }

    public String getPartType() { return partType; }
    public void setPartType(String t) { this.partType = t; }

    @Override
    public String toString() {
        return "Part{type=" + partType + ", " + super.toString() + "}";
    }
}

