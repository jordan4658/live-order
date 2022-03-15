package com.caipiao.live.order.service.order;


import java.io.Serializable;

public class OrderEvent implements Serializable {

    private String orderjson;

    public String getOrderjson() {
        return orderjson;
    }

    public void setOrderjson(String orderjson) {
        this.orderjson = orderjson;
    }
}
