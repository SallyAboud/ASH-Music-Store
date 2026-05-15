package org.musicStore.model;


public class Instruments extends Stock {
    protected String instrumentType; 

    public Instruments() { super(); }

    public Instruments(String name, String brand, double price,
                       int quantity, String category, String instrumentType) {
        super(name, brand, price, quantity, category);
        this.instrumentType = instrumentType;
    }

    public String getInstrumentType() { return instrumentType; }
    public void setInstrumentType(String t) { this.instrumentType = t; }

    @Override
    public String toString() {
        return "Instrument{type=" + instrumentType + ", " + super.toString() + "}";
    }
}

