package com.goatilicious.enums;

public enum TamanhoProduto {
    ML_125("125ml"),
    ML_500("500ml"),
    L_1("1L"),
    L_5("5L");

    private final String rotulo;

    TamanhoProduto(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
