package com.example.demo.parser;

import org.springframework.stereotype.Component;

import com.example.demo.model.CardReceiptType;

@Component
public class CardReceiptParserFactory {

    public BaseReceiptParser get(CardReceiptType type) {
        if (type == null) type = CardReceiptType.UNKNOWN;

        return switch (type) {
            case CONVENIENCE -> new ConvenienceReceiptParser();
            case COUPANG_APP, COUPANG_CARD -> new CoupangReceiptParser();
            case MART_ITEMIZED -> new MartReceiptParser();
            case DELIVERY -> new DeliveryReceiptParser();
            case CARD_SLIP_GENERIC -> new GenericCardSlipParser();
            case UNKNOWN -> new GenericCardSlipParser(); // fallback
        };
    }
}
