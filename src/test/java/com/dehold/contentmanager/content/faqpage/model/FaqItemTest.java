package com.dehold.contentmanager.content.faqpage.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FaqItemTest {

    @Test
    void gettersSettersAndEquality() {
        FaqItem a = new FaqItem();
        a.setTitle("Q");
        a.setText("A");

        assertEquals("Q", a.getTitle());
        assertEquals("A", a.getText());

        FaqItem b = new FaqItem("Q", "A");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        // inequality
        FaqItem c = new FaqItem("Q2", "A2");
        assertNotEquals(a, c);
    }
}
